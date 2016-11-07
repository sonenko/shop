package com.github.sonenko.shop.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.github.sonenko.shop.Config

/** rest routes for '/api/products'
  */
trait AdminRoute { this: RootRoute =>
  val adminRoute: Route = pathPrefix("api" / "admin") {
    authenticateBasic(realm = Config.host, basicAuthenticator) { user =>
      path("sessions") {
        get {
          complete {
            Nil
          }
        }
      }
    }
  }

  def basicAuthenticator: Authenticator[String] = {
    case p @ Credentials.Provided("admin") if p.verify("pwd") => Some("admin")
    case _ => None
  }
}