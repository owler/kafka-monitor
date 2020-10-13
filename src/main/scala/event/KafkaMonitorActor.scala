package event

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorLogging}
import akka.camel.CamelMessage
import event.json.{KMessage, Partitions, Topics}
import event.message.{ListTopics, Message, TopicDetails}
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats


class KafkaMonitorActor extends Actor with ActorLogging {
  val dataformat = "yyyy-MM-dd HH:mm:ss.SSS z"
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat(dataformat)
  }


  override def receive: Receive = {
    case msg: CamelMessage =>
      sender ! (msg.body match {
        case ListTopics(callback) => callback match {
          case null => write(Topics(Kafka.getTopics))
          case _ => new CamelMessage("/**/" + callback + "(" + write(Topics(Kafka.getTopics)) + ")", Map("content-type"->"application/x-javascript"))
        }
        case TopicDetails(topicName, callback) => callback match {
          case null => write(Partitions(Kafka.getTopic(topicName)))
          case _ => new CamelMessage("/**/" + callback + "(" + write(Partitions(Kafka.getTopic(topicName))) + ")", Map("content-type"->"application/x-javascript"))
        }
        case Message(topicName, partition, offset, callback) => callback match {
          case null => write(KMessage(new String(Kafka.getMessage(topicName, partition.toInt, offset.toLong), StandardCharsets.UTF_8)))
          case _ => new CamelMessage("/**/" + callback + "(" + write(KMessage(new String(Kafka.getMessage(topicName, partition.toInt, offset.toLong), StandardCharsets.UTF_8))) + ")", Map("content-type"->"application/x-javascript"))
        }
      })
  }
}
