package event

import java.time.Duration
import java.util
import java.util.Properties

import event.utils.CharmConfigObject
import org.apache.kafka.clients.consumer._

import collection.JavaConverters._
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.collection.mutable

object Kafka {
  val conf = CharmConfigObject
  val BOOTSTRAP_SERVERS = conf.getString("kafka.brokers")

  private def createConsumer(props: Properties = new Properties() ) = {
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
    //props.put(ConsumerConfig.GROUP_ID_CONFIG, null)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  def getTopics: mutable.Map[String, util.List[PartitionInfo]] = {
    val consumer = createConsumer()
    val list = consumer.listTopics()
    consumer.close()
    list.asScala
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
