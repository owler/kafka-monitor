package event

import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.camel.{CamelExtension, _}
import akka.routing.FromConfig
import event.ext.{PluginManager, Utf8Decoder}
import event.message._
import event.processor.{RedirectProcessor, StaticContentProcessor}
import event.utils.CharmConfigObject
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode


object KafkaMonitor {
  val staticProcessor = new StaticContentProcessor()
  val redirectProcessor = new RedirectProcessor()
  val conf = CharmConfigObject

  def main(str: Array[String]) {
    val system = ActorSystem("event-system")
    val camel = CamelExtension(system).context
    val utfDecoder = new Utf8Decoder();
    val decoders = PluginManager.loadDecoders(conf.getString("plugin.path")) + (utfDecoder.getName() -> utfDecoder)
    val monitor = system.actorOf(Props(classOf[KafkaMonitorActor], decoders).withRouter(FromConfig()), "kafka-monitor")

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

  class CustomRouteBuilder(system: ActorSystem, monitor: ActorRef) extends RouteBuilder {
    override def configure(): Unit = {
      restConfiguration.component("jetty").host("0.0.0.0").port(conf.getConfig.getInt("rest.port"))
        .bindingMode(RestBindingMode.auto)
        .enableCORS(true) // <-- Important
        .corsAllowCredentials(true) // <-- Important
        .corsHeaderProperty("Access-Control-Allow-Origin", "*")
      rest("/topic/")
        .get("/list").to("direct:listTopics")
        .get("/details?filter[filters][0][value]={id}").to("direct:topicDetails")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}/limit/{limit}").to("direct:showMessages")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}").to("direct:showMessage")
        .get("/{id}/partition/{partition}/offset/{offset}/download").produces("application/octet-stream").to("direct:downloadMessage")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}/download").produces("application/octet-stream").to("direct:downloadMessageForType")

      from("jetty:http://0.0.0.0:" + conf.getConfig.getInt("http.port") + "/topic/?matchOnUriPrefix=true").process(redirectProcessor)
      from("jetty:http://0.0.0.0:" + conf.getConfig.getInt("http.port") + "/?matchOnUriPrefix=true").process(staticProcessor)
      from("direct:listTopics").process((exchange: Exchange) =>
        exchange.getIn.setBody(ListTopics(exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:topicDetails").process((exchange: Exchange) => {
        println(exchange.getIn.getHeaders)
        exchange.getIn.setBody(TopicDetails(exchange.getIn.getHeader("filter[filters][0][value]", classOf[String]),
          exchange.getIn.getHeader("callback", classOf[String])))
      }
      ).to(monitor)

      from("direct:showMessages").process((exchange: Exchange) =>
        exchange.getIn.setBody(Messages(exchange.getIn.getHeader("id", classOf[String]),
          exchange.getIn.getHeader("partition", classOf[String]),
          exchange.getIn.getHeader("offset", classOf[String]),
          exchange.getIn.getHeader("msgtype", classOf[String]),
          exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:showMessage").process((exchange: Exchange) =>
        exchange.getIn.setBody(Message(exchange.getIn.getHeader("id", classOf[String]),
          exchange.getIn.getHeader("partition", classOf[String]),
          exchange.getIn.getHeader("offset", classOf[String]),
          exchange.getIn.getHeader("msgtype", classOf[String]),
          exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:downloadMessage")
        .process((exchange: Exchange) =>
          exchange.getIn.setBody(MessageB(exchange.getIn.getHeader("id", classOf[String]),
            exchange.getIn.getHeader("partition", classOf[String]),
            exchange.getIn.getHeader("offset", classOf[String]),
            exchange.getIn.getHeader("callback", classOf[String])))).to(monitor).convertBodyTo(classOf[Array[Byte]])
        .setHeader("Content-Disposition", simple("attachment;filename=kafka-msg.bin"));

      from("direct:downloadMessageForType")
        .process((exchange: Exchange) =>
          exchange.getIn.setBody(MessageT(exchange.getIn.getHeader("id", classOf[String]),
            exchange.getIn.getHeader("partition", classOf[String]),
            exchange.getIn.getHeader("offset", classOf[String]),
            exchange.getIn.getHeader("msgType", classOf[String]),
            exchange.getIn.getHeader("callback", classOf[String])))).to(monitor).convertBodyTo(classOf[Array[Byte]])
        .setHeader("Content-Disposition", simple("attachment;filename=kafka-msg.txt"));

    }
  }
}