package event

import java.text.SimpleDateFormat
import akka.actor.{Actor, ActorLogging}
import akka.camel.CamelMessage
import event.message.{ListTopics, Message}
import org.json4s.native.Serialization.{write}
import org.json4s.{DefaultFormats}


class KafkaMonitorActor extends Actor with ActorLogging {
  val dataformat = "yyyy-MM-dd HH:mm:ss.SSS z"
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat(dataformat)
  }

  case class Topics(topics: List[TopicMetaData])
  case class KMessage(message: Array[Byte])
  override def receive: Receive = {
    case msg: CamelMessage =>
      sender ! (msg.body match {
        case ListTopics(callback) => callback match {
          case null => write(Topics(Kafka.getTopics))
          case _ => new CamelMessage("/**/" + callback + "(" + write(Topics(Kafka.getTopics)) + ")", Map("content-type"->"application/x-javascript"))
        }
        case Message(topicName, partition, offset, callback) => callback match {
          case null => write(KMessage(Kafka.getMessage(topicName, partition.toInt, offset.toLong)))
          case _ => new CamelMessage("/**/" + callback + "(" + write(KMessage(Kafka.getMessage(topicName, partition.toInt, offset.toLong))) + ")", Map("content-type"->"application/x-javascript"))
        }
      })
  }
}
