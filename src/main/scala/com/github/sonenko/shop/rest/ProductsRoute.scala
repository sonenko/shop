package com.github.sonenko.shop.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import com.github.sonenko.shop.depot.Depot

/** rest routes for '/api/products'
  */
trait ProductsRoute { this: RootRoute =>
  val productsRoute: Route = pathPrefix("api" / "products") {
    pathEndOrSingleSlash {
      get {
        complete{
          ask(depot, Depot.Queries.GetState).mapTo[Depot.Answer]
        }
      }
    }
  }
}

