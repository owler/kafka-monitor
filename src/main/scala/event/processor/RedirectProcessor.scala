package event.processor

import java.net.InetAddress

import org.apache.camel.{Exchange, Processor}

class RedirectProcessor extends Processor{

  //this doesn't work  as its returns 127.0.0.1.  Consider get host (and port) from config
  private val host = InetAddress.getLocalHost.getHostAddress

  override def process(exchange: Exchange): Unit = {
    val in = exchange.getIn
    val relativePath = in.getHeader(Exchange.HTTP_PATH, classOf[String])
    val query = in.getHeader(Exchange.HTTP_QUERY, classOf[String])
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 302)
    exchange.getMessage().setHeader("location", s"http://$host:8877/topic/$relativePath/?$query")
  }
}