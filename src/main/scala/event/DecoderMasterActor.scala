package event

import akka.actor.{Actor, ActorLogging, ActorRef, ReceiveTimeout}
import event.json.KMessage
import concurrent.duration._

class DecoderMasterActor(respondTo: ActorRef, decoderActor: ActorRef, msgType: String, limit: Int, callback: String) extends Actor with ActorLogging {
  import context._
  import scala.language.postfixOps

  override def receive: Receive = waitingForRequest
  def waitingForRequest: Receive = {
    case list: List[KMessage[Array[Byte]]] =>
      if (list.nonEmpty) {
        list.foreach(decoderActor ! (msgType, limit, _))
        setReceiveTimeout(45 seconds)
        become(waitingForResponses(respondTo, list.length, List(), callback))
      } else {
        respondTo ! writeJson("messages" -> List(), callback)
      }
  }


  def waitingForResponses(respondTo: ActorRef, count: Int, list: List[KMessage[Array[Byte]]], callback: String): Receive = {
    case m: KMessage[Array[Byte]] =>
      if (count - 1 == 0) {
        respondTo ! writeJson("messages" -> (m :: list).sortBy(_.offset), callback)
        context stop self
      } else {
        become(waitingForResponses(respondTo, count - 1, m :: list, callback))
      }

    case ReceiveTimeout =>
      respondTo ! writeJson("messages" -> List(), callback)
      context stop self
  }

}
