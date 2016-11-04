package com.github.sonenko.shop.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


trait BasketRoute { this: RootRoute =>
  def basketRoute: Route = pathPrefix("api" / "shoppingbasket") {
    pathEndOrSingleSlash {
      get {
        complete {
          "[]"
        }
      }
    }
  }
}
