package com.github.sonenko.shoppingbasket.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/** rest routes for '/api/products'
  */
trait ProductsRoute { this: RootRoute =>
  val productsRoute: Route = pathPrefix("api" / "products") {
    pathEndOrSingleSlash {
      get {
        complete {
          depot.getState
        }
      }
    }
  }
}

