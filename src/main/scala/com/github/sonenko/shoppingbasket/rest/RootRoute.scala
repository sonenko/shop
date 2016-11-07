package com.github.sonenko.shoppingbasket.rest

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.github.sonenko.shoppingbasket.depot.Depot

import scala.concurrent.duration._

/**
  * main route that combine other routes for rest
  * @param log - logger
  * @param depot - actor that serves as Depot
  */
class RootRoute(val log: LoggingAdapter, val depot: Depot) extends JsonProtocol
    with ShoppingBasketRoute with ProductsRoute with AdminRoute {

  implicit val timeout = Timeout(10 seconds)
  def route = shoppingBasketRoute ~ productsRoute ~ adminRoute
}
