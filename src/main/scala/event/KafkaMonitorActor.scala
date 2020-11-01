package event

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.camel.CamelMessage
import com.typesafe.config.Config
import event.ext.Decoder
import event.json.{KMessage, MsgType}
import event.message._

class KafkaMonitorActor(conf: Config, decoders: Map[String, Decoder], decoderActor: ActorRef) extends Actor with ActorLogging {
  import context._
  private val truncate = conf.getInt("truncate")
  override def receive: Receive = {
    case msg: CamelMessage =>
      msg.body match {
        case ListTopics(callback) => sender ! writeJson("topics" -> Kafka.getTopics, callback)
        case ListMsgTypes(callback) => sender ! writeJson("msgtypes" -> decoders.map(d => MsgType(d._1)).toList, callback)
        case TopicDetails(topicName, callback) => sender ! writeJson("partitions" -> Kafka.getTopic(topicName), callback)

        case Messages(topicName, partition, offset, msgType, callback) =>
          val list = Kafka.getMessage(topicName, partition.toInt, offset.toLong, 10).getOrElse(List())
          val master = actorOf(Props(classOf[DecoderMasterActor], sender, decoderActor, msgType, callback))
          master ! list


        case Message(topicName, partition, offset, msgType, callback) =>
          val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
          val response = Kafka.getMessage(topicName, partition.toInt, offset.toLong).map(
            _.map(a => {
              val decoded = decode(decoder, a.message, truncate)
              val truncStr = if (decoded.bytes.length >= truncate)
                """
                  |... message truncated""".stripMargin else ""
              KMessage(a.offset, a.timestamp, new String(decoded.bytes) + truncStr, a.size, decoder.getName(), decoded.size)
            })).getOrElse(List())
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
  }

}