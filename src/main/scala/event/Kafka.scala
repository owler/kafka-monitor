package event

import java.time.Duration
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import event.json.{KMessage, Partition, Topic}
import event.utils.CharmConfigObject
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.collection.JavaConverters._
import scala.collection.mutable


case class TopicMetaData(topic: String, metadata: mutable.SortedMap[Int, (Long, Long)])

object Kafka {
  val conf = CharmConfigObject
  val BOOTSTRAP_SERVERS = conf.getString("kafka.brokers")
  var repo = new ConcurrentHashMap[String, TopicMetaData]().asScala
  refreshRepo

  private def createConsumer(props: Properties = new Properties()) = {
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
    //props.put(ConsumerConfig.GROUP_ID_CONFIG, null)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  def refreshRepo = {
    val consumer = createConsumer()
    val list = consumer.listTopics().asScala
    println(list)

    val tps: List[TopicPartition] = list.flatMap(t => t._2.asScala.map(partitionInfo => new TopicPartition(t._1, partitionInfo.partition()))).toList
    println(tps)
    repo ++= getTopicInfo(tps)
    println(repo)
    consumer.close()
  }

  def getTopics: List[Topic] = {
    refreshRepo
    repo.values.toList.sortBy(t => t.topic).map(t => Topic(t.topic))
  }

  def getTopic(topicName: String): List[Partition] = {
    repo(topicName).metadata.map(tmd => Partition(topicName, tmd._1, tmd._2._1, tmd._2._2)).toList
  }

  implicit class ToSortedMap[A, B](tuples: TraversableOnce[(A, B)])
                                  (implicit ordering: Ordering[A]) {
    def toSortedMap =
      mutable.SortedMap(tuples.toSeq: _*)
  }

  def getTopicInfo(tp: List[TopicPartition], consumer: KafkaConsumer[Array[Byte], Array[Byte]] = createConsumer()): Map[String, TopicMetaData] = {
    val startOffsets = consumer.beginningOffsets(tp.asJava).asScala
    val endOffsets = consumer.endOffsets(tp.asJava).asScala
    startOffsets.groupBy(_._1.topic()).map(x => x._1 -> TopicMetaData(x._1, x._2.map(y => y._1.partition() -> (y._2.toLong, endOffsets(y._1).toLong)).toSortedMap))
  }


  def getMessage(topic: String, partition: Int, offset: Long, count: Int = 1): Option[List[KMessage[Array[Byte]]]] = {
    repo.get(topic).flatMap(
      _.metadata.get(partition).flatMap(offsets => if (offsets._1 != offsets._2 && offset < offsets._2) Some(offset) else None)
    ) map { verifyedOffset =>
      val consumer = createConsumer()
      val tp = new TopicPartition(topic, partition)
      consumer.assign(List(tp).asJava)
      consumer.seek(tp, verifyedOffset)
      val records = consumer.poll(Duration.ofSeconds(10))
      records.iterator().asScala.take(count).map(m => KMessage(m.offset(), m.timestamp(), m.value())).toList
    }
  }
}
