package com.github.sonenko.shop

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.sonenko.shop.rest.RootRoute

object Main extends App {
  new Constructor().start()
}

class Constructor {
  implicit val system = ActorSystem("shop-system")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  val log: LoggingAdapter = Logging.getLogger(system, this)
  val rootRoute = (new RootRoute).route


  def start(): Unit = {
    Http().bindAndHandle(rootRoute, Config.host, Config.port).onSuccess {
      case binding => log.info(s"`Shop-API` started at: ${binding.localAddress}")
    }
  }
}
