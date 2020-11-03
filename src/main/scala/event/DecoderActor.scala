package event

import akka.actor.{Actor, ActorLogging}
import event.ext.Decoder
import event.json.KMessage

class DecoderActor(decoders: Map[String, Decoder]) extends Actor with ActorLogging {
  override def receive: Receive = {
    case (msgType: String, limit: Int, m: KMessage[Array[Byte]]) =>
      val start = System.currentTimeMillis()
      val decoder = decoders.getOrElse(msgType, decoders("UTF8"))
      val decoded = decode(decoder, m.message, limit)
      val truncStr = if (decoded.bytes.length >= limit)
        """
          |... message truncated""".stripMargin else ""
      log.debug(s"decoded offset: ${m.offset} with size: ${decoded.size} in ${System.currentTimeMillis() - start} ms")
      sender ! KMessage(m.offset, m.timestamp, new String(decoded.bytes) + truncStr, m.size, decoder.getName(), decoded.size)
  }

}
