package event.processor

import java.net.InetAddress

import org.apache.camel.{Exchange, Processor}

class RedirectProcessor extends Processor{
  val host = InetAddress.getLocalHost.getHostAddress

  override def process(exchange: Exchange): Unit = {
    val in = exchange.getIn
    val relativepath = in.getHeader(Exchange.HTTP_PATH, classOf[String])
    val query = in.getHeader(Exchange.HTTP_QUERY, classOf[String])
    exchange.getOut.setHeader(Exchange.HTTP_RESPONSE_CODE, 302)
    println("location: " + host)
    exchange.getOut.setHeader("location", s"http://${host}:8877/topic/${relativepath}/?${query}")
  }
}