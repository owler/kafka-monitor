import java.io
import java.text.SimpleDateFormat
import akka.camel.CamelMessage
import com.typesafe.scalalogging.Logger
import event.ext.{DecodedMessage, Decoder}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

import java.io.{PrintWriter, StringWriter}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

package object event {
  private val log = Logger(LoggerFactory.getLogger(this.getClass))
  private val dataFormat = "yyyy-MM-dd HH:mm:ss.SSS z"
  private implicit val formats: DefaultFormats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat(dataFormat)
  }

  @tailrec
  def getCause(e: Throwable, acc: String): String = {
    if (e == null) acc
    else getCause(e.getCause, acc + "\n" + e.getMessage)
  }

  def tryWithError(f:() => Any): Any = {
    try {
      f()
    } catch {
      case e: Throwable =>
        log.error("Errors", e)
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        e.printStackTrace(pw)
        "errors" -> List(getCause(e.getCause, e.getMessage), sw.toString)
    }
  }

  def writeJson(obj: Any, callback: String): io.Serializable = {
    callback match {
      case null => write(obj)
      case _ => new CamelMessage("/**/" + callback + "(" + write(obj) + ")", Map("content-type" -> "application/x-javascript"))
    }
  }

  def decode(decoder: Decoder, message: Array[Byte], limit: Int): DecodedMessage = {
    Try(decoder.decode(message, limit)) match {
      case Success(value) => if(value == null) DecodedMessage(s"${decoder.getName()} returned null".getBytes(),0) else value
      case Failure(e) => DecodedMessage(s"Unable to decode with ${decoder.getName()}: ${e.getMessage}".getBytes(),0)
    }
  }

}
