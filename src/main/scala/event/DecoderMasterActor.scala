package event

import java.io
import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import akka.camel.CamelMessage
import event.json.KMessage
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import concurrent.duration._

class DecoderMasterActor(respondTo: ActorRef, decoderActor: ActorRef, msgType: String, callback: String) extends Actor with ActorLogging {
  import context._
  import scala.language.postfixOps

  private val dataFormat = "yyyy-MM-dd HH:mm:ss.SSS z"
  private implicit val formats: DefaultFormats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat(dataFormat)
  }

  override def receive: Receive = waitingForRequest
  def waitingForRequest: Receive = {
    case list: List[KMessage[Array[Byte]]] =>
      log.info(">>>>>> Received list " + list.length)
      if (list.nonEmpty) {
        list.foreach(decoderActor ! (msgType,_))
        setReceiveTimeout(30 seconds)
        become(waitingForResponses(respondTo, list.length, List(), callback))
      } else {
        respondTo ! writeJson("messages" -> List(), callback)
      }
  }


  def waitingForResponses(respondTo: ActorRef, count: Int, list: List[KMessage[Array[Byte]]], callback: String): Receive = {
    case m: KMessage[Array[Byte]] =>
      log.info("Receive msg: " + m.offset)
      log.info("Count " + count)
      if (count - 1 == 0) {
        log.info("Responding to client  msgs: " + list.length)
        respondTo ! writeJson("messages" -> (m :: list), callback)
        context stop self
      } else {
        become(waitingForResponses(respondTo, count - 1, m :: list, callback))
      }

    case ReceiveTimeout =>
      respondTo ! writeJson("messages" -> List(), callback)
      context stop self
  }


  def writeJson(obj: Any, callback: String): io.Serializable = {
    callback match {
      case null => write(obj)
      case _ => new CamelMessage("/**/" + callback + "(" + write(obj) + ")", Map("content-type" -> "application/x-javascript"))
    }
  }
}
