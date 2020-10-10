package event

import akka.actor.{Actor, ActorLogging}
import akka.camel.CamelMessage
import event.message.{ListTopics, Message}

class KafkaMonitorActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg: CamelMessage =>
      sender ! (msg.body match {
        case msg: ListTopics => Kafka.getTopics
        case msg: Message => Kafka.getMessage(msg.topicName, msg.partition.toInt, msg.offset.toLong)
      })
  }
}

