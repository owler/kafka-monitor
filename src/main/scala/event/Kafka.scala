package event

import java.util.Properties

import org.apache.kafka.clients.consumer._
import java.util.Collections

import org.apache.kafka.common.serialization.ByteArrayDeserializer

class Kafka {

  private def createConsumer = {
    val BOOTSTRAP_SERVERS = ""
    val TOPIC= ""
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaExampleConsumer")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer].getName)
    // Create the consumer using props.
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)
    // Subscribe to the topic.
    consumer.subscribe(Collections.singletonList(TOPIC))
    consumer
  }
}
