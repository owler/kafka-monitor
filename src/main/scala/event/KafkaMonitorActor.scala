package event

import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorLogging}
import akka.camel.CamelMessage
import event.ext.Decoder
import event.json.{KMessage, KMessages, MsgType, MsgTypes, Partitions, Topics}
import event.message.{ListMsgTypes, ListTopics, Message, MessageB, MessageT, Messages, TopicDetails}
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats

import scala.util.{Failure, Success, Try}


class KafkaMonitorActor(decoders: Map[String, Decoder]) extends Actor with ActorLogging {
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
        case ListMsgTypes(callback) => callback match {
          case null => write(MsgTypes(decoders.map(d => MsgType(d._1)).toList))
          case _ => new CamelMessage("/**/" + callback + "(" + write(MsgTypes(decoders.map(d => MsgType(d._1)).toList)) + ")", Map("content-type"->"application/x-javascript"))
        }
        case TopicDetails(topicName, callback) => callback match {
          case null => write(Partitions(Kafka.getTopic(topicName)))
          case _ => new CamelMessage("/**/" + callback + "(" + write(Partitions(Kafka.getTopic(topicName))) + ")", Map("content-type"->"application/x-javascript"))
        }
        case Messages(topicName, partition, offset, msgType, callback) => {
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          val response = KMessages(Kafka.getMessage(topicName, partition.toInt, offset.toLong, 10).map(
            _.map(a => {
              val decoded = Try(decoder.decode(a.message)) match {
                case Success(value) => value
                case Failure(e) => s"Unable to decode with ${decoder.getName()}: ${e.getMessage}"
              }
              KMessage(a.offset, a.timestamp, decoded.take(500), a.size, decoder.getName(), decoded.length)
            })).getOrElse(List()))
          callback match {
            case null => write(response)
            case _ => new CamelMessage("/**/" + callback + "(" + write(response) + ")", Map("content-type"->"application/x-javascript"))
          }
        }
        case Message(topicName, partition, offset, msgType, callback) => {
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          val response = KMessages(Kafka.getMessage(topicName, partition.toInt, offset.toLong).map(
            _.map(a => {
              val decoded = Try(decoder.decode(a.message)) match {
                case Success(value) => value
                case Failure(e) => s"Unable to decode with ${decoder.getName()}: ${e.getMessage}"
              }
              val truncStr = if (decoded.length > 5000)
                """
                  |... message truncated""".stripMargin else ""
              KMessage(a.offset, a.timestamp, decoded.take(5000) + truncStr, a.size, decoder.getName(), decoded.length)})).getOrElse(List()))
          callback match {
            case null => write(response)
            case _ => new CamelMessage("/**/" + callback + "(" + write(response) + ")", Map("content-type"->"application/x-javascript"))
          }
        }
        case MessageB(topicName, partition, offset, _) => {
          Kafka.getMessage(topicName, partition.toInt, offset.toLong) match {
            case None => Array[Byte]()
            case Some(l) => l.head.message
          }
        }
        case MessageT(topicName, partition, offset, msgType, _) => {
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          Kafka.getMessage(topicName, partition.toInt, offset.toLong) match {
            case None => ""
            case Some(l) => Try(decoder.decode(l.head.message)) match {
              case Success(value) => value
              case Failure(e) => s"Unable to decode with ${decoder.getName()}: ${e.getMessage}"
            }
          }
        }
      })
  }
}
