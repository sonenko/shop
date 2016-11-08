package com.github.sonenko.shoppingbasket

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.sonenko.shoppingbasket.depot.DepotActor
import com.github.sonenko.shoppingbasket.rest.RootRoute
import com.github.sonenko.shoppingbasket.shop.ShopActor


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

  import system.dispatcher
  val log = Logging.getLogger(system, this)
  val depot = DepotActor.create(system)
  val shop = ShopActor.create(system, depot)
  val route = new RootRoute(
    log = log,
    depot = depot,
    shop = shop
  ).route

  def start(): Unit = {
    Http().bindAndHandle(route, Config.host, Config.port).onSuccess {
      case binding => log.info(s"`Shop-API` started at: ${binding.localAddress}")
    }
  }
}
