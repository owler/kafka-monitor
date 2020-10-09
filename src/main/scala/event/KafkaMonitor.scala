package event

import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.camel.{CamelExtension, _}
import akka.routing.FromConfig
import event.message.{ListTopics, Message}
import event.utils.CharmConfigObject
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode


object KafkaMonitor {

  class CustomRouteBuilder(system: ActorSystem, monitor: ActorRef) extends RouteBuilder {
    override def configure(): Unit = {
      restConfiguration.component("jetty").host("localhost").port(8877).bindingMode(RestBindingMode.auto)
      rest("/topic/")
        .get("/list").to("direct:listTopics")
        .get("/{id}/partition/{partition}/offset/{offset}").to("direct:showMessage")

      from("direct:listTopics").setBody(constant(ListTopics(null))).to(monitor)
      from("direct:showMessage").process((exchange: Exchange) =>
        exchange.getIn.setBody(Message(exchange.getIn.getHeader("id", classOf[String]),
          exchange.getIn.getHeader("partition", classOf[String]),
          exchange.getIn.getHeader("offset", classOf[String]), null))).to(monitor)
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
