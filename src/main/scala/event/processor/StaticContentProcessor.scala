package event.processor

import java.io.{File, FileInputStream}
import java.nio.file.{FileSystems, Files, Paths}

import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.apache.camel.{Exchange, Processor}
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

class StaticContentProcessor(conf: Config) extends Processor {
  val log = Logger(LoggerFactory.getLogger(this.getClass))
  val root = conf.getString("resourceBase")

  override def process(exchange: Exchange): Unit = {
    val in = exchange.getIn

    var relativepath = in.getHeader(Exchange.HTTP_PATH, classOf[String]).replaceAll("/+", "/")
    val requestPath = in.getHeader("CamelServletContextPath", classOf[String]) //CamelServletContextPath
    if (relativepath.isEmpty || relativepath == "/") relativepath = "index.html"

    val formattedPath = String.format("%s/%s", requestPath, relativepath).replaceAll("/+", "/")
    log.debug("trying " + formattedPath)

    val out = exchange.getMessage()
    try {
      /* use ResourseAsStream if you need to read from classpath */
      //val pathStream = this.getClass.getResourceAsStream(formattedPath)
      //val path = FileSystems.getDefault.getPath(Paths.get(this.getClass.getResource(formattedPath).toURI).toString)

      val file = new File(root + formattedPath)
      val pathStream = new FileInputStream(file)
      val path = FileSystems.getDefault.getPath(Paths.get(file.toURI).toString)

      out.setBody(IOUtils.toByteArray(pathStream))
      out.setHeader(Exchange.CONTENT_TYPE, Files.probeContentType(path))
    } catch {
      case e: Exception =>
        out.setBody(relativepath + " not found. " + e.getMessage)
        out.setHeader(Exchange.HTTP_RESPONSE_CODE, "404")
    }
  }
}