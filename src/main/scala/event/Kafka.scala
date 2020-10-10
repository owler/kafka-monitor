package event

import java.util
import java.util.Properties

import org.apache.kafka.clients.consumer._
import collection.JavaConverters._

import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

class Kafka {

  private def createConsumer(props: Properties = new Properties() ) = {
    val BOOTSTRAP_SERVERS = ""
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
    //props.put(ConsumerConfig.GROUP_ID_CONFIG, null)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    new KafkaConsumer[Array[Byte], Array[Byte]](props)
  }

  def getTopics: util.Map[String, util.List[PartitionInfo]] = {
    val consumer = createConsumer()
    val list = consumer.listTopics()
    consumer.close()
    list
  }

  def getMessage(topic: String, partition: Int, offset: Long) = {
    val consumer = createConsumer()
    val tp = new TopicPartition(topic, partition)
    consumer.assign(List(tp).asJava)
    consumer.seek(tp, offset)
  }
}
