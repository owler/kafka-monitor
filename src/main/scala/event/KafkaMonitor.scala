package event

import java.util.Date
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.camel.{CamelExtension, _}
import akka.routing.FromConfig
import event.ext.{PluginManager, Utf8Decoder}
import event.message.{ListMsgTypes, ListTopics, Message, MessageB, MessageT, Messages, TopicDetails}
import event.security.{KSecurityHandler, WebSSOHandler}
import event.utils.CharmConfigObject
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.SimpleRegistry
import org.apache.camel.model.rest.RestBindingMode
import org.eclipse.jetty.util.resource.Resource


object KafkaMonitor {
  private val conf = CharmConfigObject
  //private val redirectProcessor = new RedirectProcessor()

  class CustomRouteBuilder(system: ActorSystem, monitor: ActorRef) extends RouteBuilder {
    override def configure(): Unit = {
      val contextPath = conf.getString("contextPath")
      restConfiguration.component("jetty").host("0.0.0.0").port(conf.getConfig.getInt("http.port"))
        .contextPath(contextPath)
        .bindingMode(RestBindingMode.auto)
      //enable CORS if you need to use RedirectProcessor
/*
              .enableCORS(true) // <-- Important
              .corsAllowCredentials(true) // <-- Important
              .corsHeaderProperty("Access-Control-Allow-Origin","*")
      from("jetty:http://0.0.0.0:" + conf.getConfig.getInt("http.port") + "/topic/?matchOnUriPrefix=true").process(redirectProcessor)
*/

      rest("/topic/")
        .get("/list").to("direct:listTopics")
        .get("/msgtypes").to("direct:msgTypes")
        .get("/details?filter[filters][0][value]={id}").to("direct:topicDetails")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}/limit/{limit}").to("direct:showMessages")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}").to("direct:showMessage")
        .get("/{id}/partition/{partition}/offset/{offset}/download").produces("application/octet-stream").to("direct:downloadMessage")
        .get("/{id}/partition/{partition}/offset/{offset}/msgtype/{msgtype}/download").produces("application/octet-stream").to("direct:downloadMessageForType")
      // bridge unfortunately creates bottleneck  and all threads have to wait until one process requests
      //from("jetty:http://0.0.0.0:" + conf.getConfig.getInt("http.port") + "/topic/?matchOnUriPrefix=true").to("http://localhost:8877/topic?bridgeEndpoint=true")
      from("jetty:http://0.0.0.0:" + conf.getConfig.getInt("http.port") + contextPath + "?matchOnUriPrefix=true&handlers=staticHandler,authHandler")
      .to("log:com.company.camel.sample?level=TRACE&showAll=true&multiline=true")

//      from("seda:input?limitConcurrentConsumers=false&concurrentConsumers=250").to("http://localhost:8877/topic?bridgeEndpoint=true")

      from("direct:listTopics").process((exchange: Exchange) =>
        exchange.getIn.setBody(ListTopics(exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:msgTypes").process((exchange: Exchange) =>
        exchange.getIn.setBody(ListMsgTypes(exchange.getIn.getHeader("callback", classOf[String])))).to(monitor)

      from("direct:topicDetails").process((exchange: Exchange) => {
        exchange.getIn.setBody(TopicDetails(exchange.getIn.getHeader("filter[filters][0][value]", classOf[String]),
          exchange.getIn.getHeader("callback", classOf[String])))}
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
        .process((exchange: Exchange) => {
          exchange.getIn.setBody(MessageB(exchange.getIn.getHeader("id", classOf[String]),
            exchange.getIn.getHeader("partition", classOf[String]),
            exchange.getIn.getHeader("offset", classOf[String]),
            exchange.getIn.getHeader("callback", classOf[String])))
          exchange.getMessage.setHeaders(exchange.getIn.getHeaders)
        })
        .to(monitor).convertBodyTo(classOf[Array[Byte]])
        .setHeader("Content-Disposition",
          simple("attachment;filename=${in.headers[id]}-${in.headers[partition]}-${in.headers[offset]}.bin"))

      from("direct:downloadMessageForType")
        .process((exchange: Exchange) => {
          exchange.getIn.setBody(MessageT(exchange.getIn.getHeader("id", classOf[String]),
            exchange.getIn.getHeader("partition", classOf[String]),
            exchange.getIn.getHeader("offset", classOf[String]),
            exchange.getIn.getHeader("msgType", classOf[String]),
            exchange.getIn.getHeader("callback", classOf[String])))
          exchange.getMessage.setHeaders(exchange.getIn.getHeaders)
        }).to(monitor).convertBodyTo(classOf[Array[Byte]])
        .setHeader("Content-Disposition",
            simple("attachment;filename=${in.headers[id]}-${in.headers[partition]}-${in.headers[offset]}.txt"))
    }
  }


  def main(str: Array[String]): Unit = {
    val system = ActorSystem("event-system")
    val camel = CamelExtension(system).context
    val utfDecoder = new Utf8Decoder()
    val decoders = Map(utfDecoder.getName() -> utfDecoder) ++  PluginManager.loadDecoders(conf.getString("plugin.path"))
    val decoderActor = system.actorOf(Props(classOf[DecoderActor], decoders).withRouter(FromConfig()), "kafka-decoder")
    val monitor = system.actorOf(Props(classOf[KafkaMonitorActor], conf.getConfig, decoders, decoderActor).withRouter(FromConfig()), "kafka-monitor")

    val registry = new SimpleRegistry()
    registry.put("authHandler", new KSecurityHandler(conf.getConfig.getBoolean("security.enabled")))
    //registry.put("authHandler", new WebSSOHandler(conf.getConfig.getBoolean("security.enabled")))
    val staticHandler = new org.eclipse.jetty.server.handler.ResourceHandler()
    staticHandler.setBaseResource(Resource.newResource("../web"))
    staticHandler.setWelcomeFiles(Array("index.html"))
    staticHandler.setDirectoriesListed(false)
    registry.put("staticHandler", staticHandler)
    camel.setRegistry(registry)

    camel.addRoutes(new CustomRouteBuilder(system, monitor))
    println("KafkaMonitor Service startup in " + new Date())

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        println("Shutdown in progress ...")
        system.terminate()
        println("Shutdown done")
      }
    })
  }
}