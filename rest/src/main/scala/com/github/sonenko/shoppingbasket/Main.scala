package com.github.sonenko.shoppingbasket

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.sonenko.shoppingbasket.basketmanager.{BasketManager, BasketManagerActor}
import com.github.sonenko.shoppingbasket.rest.RootRoute
import com.github.sonenko.shoppingbasket.stock.{Stock, StockActor}


/** Entry point
  */
object Main extends App {
  new Constructor()(ActorSystem("Shopping-basket-system")).start()
}

/** Creates instances of all parts of the application and finally starts akka.http server that routes with RootRoute
  */
class Constructor(implicit val system: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val log: LoggingAdapter = Logging.getLogger(system, this)
  val stock: Stock = StockActor.create(system)
  val basketManager: BasketManager = BasketManagerActor.create(system, stock)
  val route: Route = new RootRoute(
    log = log,
    stock = stock,
    basketManager = basketManager
  ).route

  def start(): Unit = {
    Http().bindAndHandle(route, Config.host, Config.port).onSuccess {
      case binding => log.info(s"`Shopping-Basket-API` started at: ${binding.localAddress}")
    }
  }
}
