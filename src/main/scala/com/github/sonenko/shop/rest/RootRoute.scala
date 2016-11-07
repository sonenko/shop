package com.github.sonenko.shop.rest

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import scala.concurrent.duration._

/**
  * main route that combine other routes for rest
  * @param log - logger
  * @param depot - actor that serves as Depot
  */
class RootRoute(val log: LoggingAdapter, val depot: ActorRef) extends JsonProtocol
    with ShoppingBasketRoute with ProductsRoute with AdminRoute {

  implicit val timeout = Timeout(10 seconds)
  def route = shoppingBasketRoute ~ productsRoute ~ adminRoute
}
