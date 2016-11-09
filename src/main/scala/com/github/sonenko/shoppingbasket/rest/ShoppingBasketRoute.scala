package com.github.sonenko.shoppingbasket.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.sonenko.shoppingbasket.Config
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor


trait ShoppingBasketRoute {
  this: RootRoute =>

  def cook = HttpCookie(Config.cookieNameForSession, java.util.UUID.randomUUID().toString)

  def shoppingBasketRoute: Route = pathPrefix("api" / "shoppingbasket") {
    cookie(Config.cookieNameForSession) { cook =>
      val basketId = java.util.UUID.fromString(cook.value)
      pathEndOrSingleSlash {
        get {
          complete {
            inquire(basketManager.actor, BasketManagerActor.Commands.ToBasket(
              basketId,
              BasketActor.Commands.GetState,
              false
            ))
          }
        } ~
        post {
          entity(as[AddGood]) { addGood =>
            complete {
              inquire(basketManager.actor, BasketManagerActor.Commands.ToBasket(
                basketId,
                BasketActor.Commands.AddGood(addGood.goodId, addGood.count),
                true
              ))
            }
          }
        } ~
        delete {
          entity(as[DropGood]) { dropGood =>
            complete {
              inquire(basketManager.actor, BasketManagerActor.Commands.ToBasket(
                basketId,
                BasketActor.Commands.DropGood(dropGood.goodId, dropGood.count),
                false
              ))
            }
          }
        }
      } ~ path("buy") {
        post {
          complete {
            inquire(basketManager.actor, BasketManagerActor.Commands.Buy(basketId))
          }
        }
      }
    } ~
    setCookie(cook) { ctx =>
      redirect(ctx.request.uri, StatusCodes.PermanentRedirect).apply(ctx)
    }
  }
}