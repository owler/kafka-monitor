package event

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.{Date, Properties}

import com.typesafe.scalalogging.Logger
import event.json.{KMessage, Partition, Topic}
import event.utils.CharmConfigObject
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.collection.{IterableOnce, mutable}


case class TopicMetaData(topic: String, metadata: mutable.SortedMap[Int, (Long, Long)])

case class MessagePosition(topic: String, partition: Int, offset: Long)

object Kafka {
  val log = Logger(LoggerFactory.getLogger(this.getClass))
  val conf = CharmConfigObject
  val cacheTime = conf.getConfig.getLong("cache.ttl")
  var repo = new ConcurrentHashMap[String, TopicMetaData]().asScala
  var repoRefreshTimestamp = new AtomicLong(0)
  val messageCache = LRUCache[MessagePosition, KMessage[Array[Byte]]](100)
  refreshRepo


  private def createConsumer(props: Properties = new Properties()) = {
    props.putAll(conf.parse("kafka").transform((_,v) => v.toString).asJava)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  private def refreshRepo = {
    repo.synchronized {
      if (rotten()) {
        try {
          val consumer = createConsumer()
          val list = consumer.listTopics().asScala
          val tps: List[TopicPartition] = list.flatMap(t => t._2.asScala.map(partitionInfo => new TopicPartition(t._1, partitionInfo.partition()))).toList
          repo ++= getTopicInfo(tps, consumer)
          repoRefreshTimestamp.set(System.currentTimeMillis())
          consumer.close()
        } catch {
          case e: Throwable => log.error("Unable refresh repo: ", e)
        }
      }
    }
  }

  def rotten(): Boolean = {
    repoRefreshTimestamp.get() + cacheTime < System.currentTimeMillis()
  }

  def getTopics: List[Topic] = {
    if (rotten()) {
      refreshRepo
    }
    repo.values.toList.sortBy(t => t.topic).map(t => Topic(t.topic))
  }

  def getTopic(topicName: String): List[Partition] = {
    repo(topicName).metadata.map(tmd => Partition(topicName, tmd._1, tmd._2._1, tmd._2._2)).toList
  }

  implicit class ToSortedMap[A, B](tuples: IterableOnce[(A, B)])
                                  (implicit ordering: Ordering[A]) {
    def toSortedMap = mutable.SortedMap() ++ tuples
  }


  private def getTopicInfo(tp: List[TopicPartition], consumer: KafkaConsumer[Array[Byte], Array[Byte]]): Map[String, TopicMetaData] = {
    val startOffsets = consumer.beginningOffsets(tp.asJava).asScala
    val endOffsets = consumer.endOffsets(tp.asJava).asScala
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
        _.metadata.get(partition).flatMap(offsets => if (offsets._1 != offsets._2 && offset >= offsets._1 && offset < offsets._2) Some(offset) else None)
      ) map { verifiedOffset =>
        val consumer = createConsumer()
        val tp = new TopicPartition(topic, partition)
        consumer.assign(List(tp).asJava)
        consumer.seek(tp, verifiedOffset)
        val records = consumer.poll(Duration.ofSeconds(10))
        val resp = records.iterator().asScala.take(count).map(m => KMessage(m.offset(), new Date(m.timestamp()), m.value(), m.value().length, null, 0)).toList.take(count)
        resp.foreach(m => messageCache += MessagePosition(topic, partition, m.offset) -> m)
        consumer.close()
        resp
      }
    } catch {
      case e: Throwable => log.error("Unable get message: ", e); None
    }
  }
}