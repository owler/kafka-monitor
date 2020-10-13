package event

import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.camel.{CamelExtension, _}
import akka.routing.FromConfig
import event.message.{ListTopics, Message, Messages, TopicDetails}
import event.processor.StaticContentProcessor
import event.utils.CharmConfigObject
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode


object KafkaMonitor {
  val staticProcessor = new StaticContentProcessor()

  class CustomRouteBuilder(system: ActorSystem, monitor: ActorRef) extends RouteBuilder {
    override def configure(): Unit = {
      restConfiguration.component("jetty").host("localhost").port(8877).bindingMode(RestBindingMode.auto)
      rest("/topic/")
        .get("/list").to("direct:listTopics")
        .get("/details?filter[filters][0][value]={id}").to("direct:topicDetails")
        .get("/{id}/partition/{partition}/offset/{offset}").to("direct:showMessages")
        .get("/{id}/partition/{partition}/offset/{offset}/download").produces("application/octet-stream").to("direct:downloadMessage")

      from("jetty:http://0.0.0.0:8081/?matchOnUriPrefix=true").process(staticProcessor)
      from("direct:listTopics").process((exchange: Exchange) =>
        exchange.getIn.setBody(ListTopics(exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:topicDetails").process((exchange: Exchange) => {
        println(exchange.getIn.getHeaders)
        exchange.getIn.setBody(TopicDetails(exchange.getIn.getHeader("filter[filters][0][value]", classOf[String]),
          exchange.getIn.getHeader("callback", classOf[String])))}
      ).to(monitor)

      from("direct:showMessages").process((exchange: Exchange) =>
        exchange.getIn.setBody(Messages(exchange.getIn.getHeader("id", classOf[String]),
          exchange.getIn.getHeader("partition", classOf[String]),
          exchange.getIn.getHeader("offset", classOf[String]), exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:downloadMessage")
        .process((exchange: Exchange) =>
        exchange.getIn.setBody(Message(exchange.getIn.getHeader("id", classOf[String]),
          exchange.getIn.getHeader("partition", classOf[String]),
          exchange.getIn.getHeader("offset", classOf[String]), exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)
        .setHeader("Content-Disposition", simple("attachment;filename=kafka-msg.bin"));
    }


  }


  val conf = CharmConfigObject

  def main(str: Array[String]) {
    val system = ActorSystem("event-system")
    val camel = CamelExtension(system).context
    val monitor = system.actorOf(Props(classOf[KafkaMonitorActor]).withRouter(FromConfig()), "kafka-monitor")

    camel.addRoutes(new CustomRouteBuilder(system, monitor))
    println("KafkaMonitor Service startup in " + new Date())

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        println("Shutdown in progress ...")
        system.terminate()
        println("Shutdown done")
      }
    })
  }
}
