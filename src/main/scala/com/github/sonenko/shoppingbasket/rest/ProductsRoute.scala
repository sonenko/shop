package com.github.sonenko.shoppingbasket
package rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.sonenko.shoppingbasket.depot.DepotActor

/** rest routes for '/api/products'
  */
trait ProductsRoute {
  this: RootRoute =>
  val productsRoute: Route = pathPrefix("api" / "products") {
    pathEndOrSingleSlash {
      get {
        complete {
          inquire(depot.actor, DepotActor.Commands.GetState)
        }
      }
    }
  }
}

