package event

import java.io
import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import akka.camel.CamelMessage
import akka.routing.FromConfig
import com.typesafe.config.Config
import event.ext.{DecodedMessage, Decoder}
import event.json.{KMessage, MsgType}
import event.message.{ListMsgTypes, ListTopics, Message, MessageB, MessageT, Messages, TopicDetails}
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats

import scala.util.{Failure, Success, Try}
import concurrent.duration._

class KafkaMonitorActor(conf: Config, decoders: Map[String, Decoder]) extends Actor with ActorLogging {
  import context._
  import scala.language.postfixOps
  private val truncate = conf.getInt("truncate")
  private val dataFormat = "yyyy-MM-dd HH:mm:ss.SSS z"
  private implicit val formats: DefaultFormats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat(dataFormat)
  }


  override def receive: Receive = {
    case msg: CamelMessage =>
      msg.body match {
        case ListTopics(callback) => sender ! writeJson("topics" -> Kafka.getTopics, callback)
        case ListMsgTypes(callback) => sender ! writeJson("msgtypes" -> decoders.map(d => MsgType(d._1)).toList, callback)
        case TopicDetails(topicName, callback) => sender ! writeJson("partitions" -> Kafka.getTopic(topicName), callback)

        case Messages(topicName, partition, offset, msgType, callback) =>
          val list = Kafka.getMessage(topicName, partition.toInt, offset.toLong, 10).getOrElse(List())
          val decoderActor = actorOf(Props(classOf[DecoderActor], decoders, 500).withRouter(FromConfig()), "kafka-decoder")
          list.foreach(decoderActor ! _)
          setReceiveTimeout(30 seconds)
          become(waitingForResponses(sender, list.length, List(), callback))

        case Message(topicName, partition, offset, msgType, callback) =>
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          val response = Kafka.getMessage(topicName, partition.toInt, offset.toLong).map(
            _.map(a => {
              val decoded = decode(decoder, a.message, truncate)
              val truncStr = if (decoded.bytes.length >= truncate)
                """
                  |... message truncated""".stripMargin else ""
              KMessage(a.offset, a.timestamp, new String(decoded.bytes) + truncStr, a.size, decoder.getName(), decoded.size)})).getOrElse(List())
          sender ! writeJson("messages" -> response, callback)

        case MessageB(topicName, partition, offset, _) =>
          Kafka.getMessage(topicName, partition.toInt, offset.toLong) match {
            case None => sender ! Array[Byte]()
            case Some(l) => sender ! l.head.message
          }

        case MessageT(topicName, partition, offset, msgType, _) =>
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          Kafka.getMessage(topicName, partition.toInt, offset.toLong) match {
            case None => sender ! ""
            case Some(l) => sender ! decode(decoder, l.head.message, Int.MaxValue).bytes
          }

      }

      def waitingForResponses(respondTo: ActorRef, count: Int, list: List[KMessage[Array[Byte]]], callback: String): Receive =  {
        case m: KMessage[Array[Byte]] =>
          if(count -1 == 0) {
            respondTo ! writeJson("messages" -> m::list, callback)
            context stop self
          } else {
            waitingForResponses(respondTo, count -1, m::list, callback)
          }

        case ReceiveTimeout =>
          respondTo ! List()
          context stop self
      }
  }

  def writeJson(obj: Any, callback: String): io.Serializable = {
    callback match {
      case null => write(obj)
      case _ => new CamelMessage("/**/" + callback + "(" + write(obj) + ")", Map("content-type"->"application/x-javascript"))
    }
  }

  def decode(decoder: Decoder, message: Array[Byte], limit: Int): DecodedMessage = {
    Try(decoder.decode(message, limit)) match {
      case Success(value) => if(value == null) DecodedMessage(s"${decoder.getName()} returned null".getBytes(),0) else value
      case Failure(e) => DecodedMessage(s"Unable to decode with ${decoder.getName()}: ${e.getMessage}".getBytes(),0)
    }
  }
}