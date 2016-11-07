package com.github.sonenko.shoppingbasket

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.rest.RootRoute
import com.github.sonenko.shoppingbasket.shop.Shop

import scala.concurrent.duration._


/** Entry point
  */
object Main extends App {
  new Constructor().start()
}

/** Constructs all together and run the application
  */
class Constructor {
  implicit val system = ActorSystem("shop-system")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(10 seconds)

  import system.dispatcher
  val log = Logging.getLogger(system, this)
  val depot = new Depot(system, timeout)//system.actorOf(Depot.props)
  val shop = new Shop(system, depot, timeout)
  val route = new RootRoute(
    log = log,
    depot = depot
  ).route

  def start(): Unit = {
    Http().bindAndHandle(route, Config.host, Config.port).onSuccess {
      case binding => log.info(s"`Shop-API` started at: ${binding.localAddress}")
    }
  }
}
