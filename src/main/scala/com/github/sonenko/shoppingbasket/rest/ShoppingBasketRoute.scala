package com.github.sonenko.shoppingbasket.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.sonenko.shoppingbasket.Config


trait ShoppingBasketRoute { this: RootRoute =>

  def cook = HttpCookie(Config.cookieNameForSession, java.util.UUID.randomUUID().toString)

  def shoppingBasketRoute: Route = pathPrefix("api" / "shoppingbasket") {
    cookie(Config.cookieNameForSession) { cook =>
      pathEndOrSingleSlash {
        get {
          complete {
            "[]"
          }
        }
      }
    } ~
    setCookie(cook) { ctx =>
      redirect(ctx.request.uri, StatusCodes.PermanentRedirect).apply(ctx)
    }
  }
}
