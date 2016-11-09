package com.github.sonenko.shoppingbasket.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor
import com.github.sonenko.shoppingbasket.{BasketNotFoundError, BasketState, Config}


trait ShoppingBasketRoute { this: RootRoute =>

  def newCookie = HttpCookie(Config.cookieNameForSession, java.util.UUID.randomUUID().toString)

  def shoppingBasketRoute: Route = pathPrefix("api" / "shoppingbasket") {
    optionalCookie(Config.cookieNameForSession) {
      case None =>
        setCookie(newCookie) { ctx =>
          redirect(ctx.request.uri, StatusCodes.PermanentRedirect).apply(ctx)
        }
      case Some(cook) =>
        val basketId = java.util.UUID.fromString(cook.value)
        pathEndOrSingleSlash {
          get {
            complete {
              inquire(basketManager.actor, BasketManagerActor.Commands.ToBasket(
                  basketId,
                  BasketActor.Commands.GetState,
                  false
                ), {case BasketNotFoundError => StatusCodes.OK -> BasketState(Nil)}
              )
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
        }
    }
  }
}