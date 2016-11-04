package com.github.sonenko.shop.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


trait ShoppingBasketRoute { this: RootRoute =>
  def shoppingBasketRoute: Route = pathPrefix("api" / "shoppingbasket") {
    pathEndOrSingleSlash {
      get {
        complete {
          "[]"
        }
      }
    }
  }
}
