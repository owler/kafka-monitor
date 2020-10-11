package event.processor

import java.nio.file.Paths
import org.apache.camel.{Exchange, Processor}
import org.apache.commons.io.IOUtils

class StaticContentProcessor extends Processor {
  override def process(exchange: Exchange): Unit = {
    import org.apache.camel.Exchange
    import java.io.IOException
    import java.io.InputStream
    import java.nio.file.FileSystems
    import java.nio.file.Files
    import java.nio.file.Path
    val in = exchange.getIn

    var relativepath = in.getHeader(Exchange.HTTP_PATH, classOf[String])
    val requestPath = in.getHeader("CamelServletContextPath", classOf[String]) //CamelServletContextPath
    println("relativepath: " + relativepath)
    if (relativepath.isEmpty || relativepath == "/") relativepath = "index.html"

    val formattedPath = String.format("%s%s", requestPath, relativepath)
    println(formattedPath)
    val pathStream = this.getClass.getResourceAsStream(formattedPath)
    println("pathStream: " + pathStream)
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
