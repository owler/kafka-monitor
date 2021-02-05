package event

import com.typesafe.scalalogging.Logger
import event.json.{KMessage, Partition, Topic}
import event.utils.CharmConfigObject
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.{Date, Properties}
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.collection.{IterableOnce, mutable}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}


case class TopicMetaData(topic: String, metadata: mutable.SortedMap[Int, (Long, Long)])

case class MessagePosition(topic: String, partition: Int, offset: Long)

object Kafka {
  private val log = Logger(LoggerFactory.getLogger(this.getClass))
  private val conf = CharmConfigObject
  private val cacheTime = conf.getConfig.getLong("cache.ttl")
  private val verbose = conf.getConfig.getBoolean("verbose")
  private val ignore = conf.getString("ignore").split(",").map(_.trim)
  private var repo = new ConcurrentHashMap[String, TopicMetaData]().asScala
  private val repoRefreshTimestamp = new AtomicLong(0)
  private val messageCache = LRUCache[MessagePosition, KMessage[Array[Byte]]](conf.getConfig.getInt("cache.size"))

  private def createConsumer(props: Properties = new Properties()) = {
    props.putAll(conf.parse("kafka").asJava)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  private def refreshRepo(): Unit = {
    repo.synchronized {
      if (rotten()) {
        Using.resource(createConsumer()) { consumer =>
          log.debug("Start refreshing Repo")
          val list = consumer.listTopics().asScala.filter(el => !ignore.contains(el._1))
          log.debug(s"Found ${list.size} topics")

          val tps: List[TopicPartition] = list.flatMap(t => t._2.asScala.map(partitionInfo => new TopicPartition(t._1, partitionInfo.partition()))).toList
          if (!verbose) {
            val tmpRepo = getTopicInfo(tps, consumer, Duration.ofSeconds(60))
            repo.clear()
            repo ++= tmpRepo
          } else {
            val tmpRepo = new ConcurrentHashMap[String, TopicMetaData]().asScala
            val ignoredTopics = ListBuffer.empty[String]
            list.foreach(t => {
              val topicPartitions = t._2.asScala.map(partitionInfo => new TopicPartition(t._1, partitionInfo.partition())).toList
              log.debug(s"Getting partition info for ${t._1}")
              Try(getTopicInfo(topicPartitions, consumer, Duration.ofSeconds(7))) match {
                case Success(value) => tmpRepo ++= value
                case Failure(e) =>
                  log.warn(s"Ignoring topic ${t._1}", e)
                  ignoredTopics += t._1
              }
            })
            if(ignoredTopics.nonEmpty) log.info(s"Ignoring topics: ${ignoredTopics.mkString(", ")}")
            repo.clear()
            repo ++= tmpRepo
          }
          repoRefreshTimestamp.set(System.currentTimeMillis())
          log.debug("Repo refreshed")
        }
      }
    }
  }

  def rotten(): Boolean = {
    repoRefreshTimestamp.get() + cacheTime < System.currentTimeMillis()
  }

  def getTopics: List[Topic] = {
    if (rotten()) {
      refreshRepo()
    }
    repo.values.toList.sortBy(t => t.topic).map(t => Topic(t.topic, t.metadata.exists(e => e._2._1 < e._2._2)))
  }

  def getTopic(topicName: String): List[Partition] = {
    repo(topicName).metadata.map(tmd => Partition(topicName, tmd._1, tmd._2._1, tmd._2._2)).toList
  }

  implicit class ToSortedMap[A, B](tuples: IterableOnce[(A, B)])
                                  (implicit ordering: Ordering[A]) {
    def toSortedMap: mutable.SortedMap[A, B] = mutable.SortedMap() ++ tuples
  }


  private def getTopicInfo(tp: List[TopicPartition], consumer: KafkaConsumer[Array[Byte], Array[Byte]], duration: Duration): Map[String, TopicMetaData] = {
    val startOffsets = consumer.beginningOffsets(tp.asJava, duration).asScala
    log.debug(s"Extracted ${startOffsets.size} start offsets")
    val endOffsets = consumer.endOffsets(tp.asJava, duration).asScala
    log.debug(s"Extracted ${endOffsets.size} end offsets")
    startOffsets.groupBy(_._1.topic()).map(x => x._1 -> TopicMetaData(x._1, x._2.map(y => y._1.partition() -> (y._2.toLong, endOffsets(y._1).toLong)).toSortedMap))
  }


  def getMessage(topic: String, partition: Int, offset: Long, count: Int = 1): Option[List[KMessage[Array[Byte]]]] = {
    if (count == 1) {
      messageCache.get(MessagePosition(topic, partition, offset)) match {
        case Some(m) => Some(List(m))
        case None => loadFromKafka(topic, partition, offset, count)
      }
    } else {
      loadFromKafka(topic, partition, offset, count)
    }
  }

  def loadFromKafka(topic: String, partition: Int, offset: Long, count: Int = 1): Option[List[KMessage[Array[Byte]]]] = {
    try {
      repo.get(topic).flatMap(
        _.metadata.get(partition).flatMap(offsets => if (offsets._1 != offsets._2 && offset >= offsets._1 && offset < offsets._2) Some((offset, offsets._2)) else None)
      ) map { verifiedOffsets =>
        Using.resource(createConsumer()) { consumer =>
          val tp = new TopicPartition(topic, partition)
          consumer.assign(List(tp).asJava)
          consumer.seek(tp, verifiedOffsets._1)
          val recount = Math.min(count, verifiedOffsets._2 - verifiedOffsets._1).toInt
          val resp = poll(consumer, recount, List())
          resp.foreach(m => messageCache += MessagePosition(topic, partition, m.offset) -> m)
          resp
        }
      }
    } catch {
      case e: Throwable => log.error("Unable get message: ", e); None
    }
  }

  @tailrec
  private def poll(consumer: KafkaConsumer[Array[Byte], Array[Byte]], count: Int, aggr: List[KMessage[Array[Byte]]]): List[KMessage[Array[Byte]]] = {
    if (count <= 0) {
      aggr
    } else {
      val records = consumer.poll(Duration.ofSeconds(10))
      val resp = records.iterator().asScala.take(count).map(m => KMessage(m.offset(), new Date(m.timestamp()), m.value(), m.value().length, null, 0)).toList
      if (resp.isEmpty) {
        aggr
      } else {
        poll(consumer, count - resp.length, resp ::: aggr)
      }
    }
  }
}