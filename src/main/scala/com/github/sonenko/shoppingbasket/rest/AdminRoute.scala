package com.github.sonenko.shoppingbasket
package rest

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor

/** rest routes for '/api/products'
  */
trait AdminRoute {
  this: RootRoute =>
  val adminRoute: Route = pathPrefix("api" / "admin" / "baskets") {
    authenticateBasic(realm = Config.host, basicAuthenticator) { user =>
      pathEndOrSingleSlash {
        get {
          complete {
            inquire(basketManager.actor, BasketManagerActor.Commands.GetState)
          }
        }
      } ~
      path(JavaUUID) { basketId =>
        get {
          complete{
            inquire(basketManager.actor, BasketManagerActor.Commands.ToBasket(
              basketId,
              BasketActor.Commands.GetState,
              false
            ), {case BasketNotFoundError => StatusCodes.NotFound})
          }
        } ~
        delete {
          complete {
            inquire(basketManager.actor, BasketManagerActor.Commands.DropBasket(basketId, false),
              {case BasketNotFoundError => StatusCodes.NotFound})
          }
        }
      }
    }
  }

  def basicAuthenticator: Authenticator[String] = {
    case p@Credentials.Provided(Config.adminName) if p.verify(Config.adminPassword) => Some(Config.adminName)
    case _ => None
  }
}