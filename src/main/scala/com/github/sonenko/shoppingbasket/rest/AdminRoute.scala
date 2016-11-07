package com.github.sonenko.shoppingbasket.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.github.sonenko.shoppingbasket.Config

/** rest routes for '/api/products'
  */
trait AdminRoute { this: RootRoute =>
  val adminRoute: Route = pathPrefix("api" / "admin") {
    authenticateBasic(realm = Config.host, basicAuthenticator) { user =>
      path("sessions") {
        get {
          complete {
            shop.listBaskets
          }
        }
      }
    }
  }

  def basicAuthenticator: Authenticator[String] = {
    case p @ Credentials.Provided(Config.adminName) if p.verify(Config.adminPassword) => Some(Config.adminName)
    case _ => None
  }
}