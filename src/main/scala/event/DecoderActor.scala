package event

import akka.actor.{Actor, ActorLogging}
import event.ext.{DecodedMessage, Decoder}
import event.json.KMessage

import scala.util.{Failure, Success, Try}

class DecoderActor(decoders: Map[String, Decoder], limit: Int) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m: KMessage[Array[Byte]] =>
      val decoder = decoders.getOrElse(m.msgtype, decoders("UTF8"))
      val decoded = decode(decoder, m.message, limit)
      val truncStr = if (decoded.bytes.length >= limit)
        """
          |... message truncated""".stripMargin else ""
      sender ! KMessage(m.offset, m.timestamp, new String(decoded.bytes) + truncStr, m.size, decoder.getName(), decoded.size)
  }

  private def decode(decoder: Decoder, message: Array[Byte], limit: Int): DecodedMessage = {
    Try(decoder.decode(message, limit)) match {
      case Success(value) => if(value == null) DecodedMessage(s"${decoder.getName()} returned null".getBytes(),0) else value
      case Failure(e) => DecodedMessage(s"Unable to decode with ${decoder.getName()}: ${e.getMessage}".getBytes(),0)
    }
  }

}
