package event

import akka.actor.{Actor, ActorLogging}
import akka.camel.CamelMessage
import event.message.{ListTopics, Message}

class KafkaMonitorActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg: CamelMessage =>
      sender ! (msg.body match {
        case body: ListTopics => "received %s" format body.callback
        case body: Message => "received %s" format body.topicName
      })
  }
}

