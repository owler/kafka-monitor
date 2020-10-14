package event.processor

import java.nio.file.Paths

import org.apache.camel.{Exchange, Processor}
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.file.{FileSystems, Files}

import com.typesafe.scalalogging.Logger
import org.apache.camel.Exchange
import org.slf4j.LoggerFactory

class StaticContentProcessor extends Processor {
  val log = Logger(LoggerFactory.getLogger(this.getClass))
  override def process(exchange: Exchange): Unit = {
    val in = exchange.getIn

    var relativepath = in.getHeader(Exchange.HTTP_PATH, classOf[String]).replaceAll("/+", "/")
    val requestPath = in.getHeader("CamelServletContextPath", classOf[String]) //CamelServletContextPath
    if (relativepath.isEmpty || relativepath == "/") relativepath = "index.html"

    val formattedPath = String.format("%s/%s", requestPath, relativepath).replaceAll("/+", "/")
    log.debug("trying " + formattedPath)
    val pathStream = this.getClass.getResourceAsStream(formattedPath)
    val path = FileSystems.getDefault.getPath(Paths.get(this.getClass.getResource(formattedPath).toURI).toString)

    val out = exchange.getOut
    try {
      out.setBody(IOUtils.toByteArray(pathStream))
      out.setHeader(Exchange.CONTENT_TYPE, Files.probeContentType(path))
    } catch {
      case e: IOException =>
        out.setBody(relativepath + " not found." + e.getMessage)
        out.setHeader(Exchange.HTTP_RESPONSE_CODE, "404")
    }
  }
}
