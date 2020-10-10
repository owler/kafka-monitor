package event

import java.time.Duration
import java.{lang, util}
import java.util.Properties

import event.utils.CharmConfigObject
import org.apache.kafka.clients.consumer._

import collection.JavaConverters._
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.collection.mutable

case class TopicMetaData(topic: String, metadata: Map[Int, (Long, Long)])

object Kafka {
  val conf = CharmConfigObject
  val BOOTSTRAP_SERVERS = conf.getString("kafka.brokers")
  var repo: Map[String, TopicMetaData] = Map[String, TopicMetaData]()
  refreshRepo

  private def createConsumer(props: Properties = new Properties() ) = {
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
    //props.put(ConsumerConfig.GROUP_ID_CONFIG, null)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  def refreshRepo = {
    val consumer = createConsumer()
    val list = consumer.listTopics()
    repo ++ list.asScala.map(t => TopicMetaData(t._1,
      getTopicInfo(t._2.asScala.map(pi => new TopicPartition(t._1, pi.partition())).toList, consumer)))
    consumer.close()
  }

  def getTopics: List[String] = {
    repo.keys.toList
  }

  def getTopicInfo(tp: List[TopicPartition], consumer: KafkaConsumer[Array[Byte], Array[Byte]] = createConsumer()): Map[Int, (Long, Long)] = {
    val startOffsets = consumer.beginningOffsets(tp.asJava).asScala
    val endOffsets = consumer.endOffsets(tp.asJava).asScala
    startOffsets.map(x => x._1.partition() -> (x._2.toLong, endOffsets(x._1).toLong)).toMap
  }

  def getMessage(topic: String, partition: Int, offset: Long): Array[Byte] = {
    val consumer = createConsumer()
    val tp = new TopicPartition(topic, partition)
    consumer.assign(List(tp).asJava)
    consumer.seek(tp, offset)
    val records = consumer.poll(Duration.ofSeconds(10))
    val it = records.iterator()
    if (it.hasNext) {
      val record = it.next()
      record.value()
    } else null
  }
}
