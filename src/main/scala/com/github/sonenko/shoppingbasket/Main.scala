package com.github.sonenko.shoppingbasket

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor
import com.github.sonenko.shoppingbasket.rest.RootRoute
import com.github.sonenko.shoppingbasket.stock.StockActor


/** Entry point
  */
object Main extends App {
  new Constructor().start()
}

/** Constructs all together and run the application
  */
class Constructor {
  implicit val system = ActorSystem("shopping-basket-system")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val log = Logging.getLogger(system, this)
  val stock = StockActor.create(system)
  val basketManager = BasketManagerActor.create(system, stock)
  val route = new RootRoute(
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
