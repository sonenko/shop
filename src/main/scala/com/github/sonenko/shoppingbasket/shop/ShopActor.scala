package com.github.sonenko.shoppingbasket
package shop

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.basket.{Basket, BasketActor}

class ShopActor(depot: Depot, createBasketFunc: (ActorRefFactory, Depot) => Basket) extends Actor {
  var baskets: Map[UUID, Basket] = Map()

  override def receive: Receive = {
    case c: ShopActor.Command => c match {
      case ShopActor.Commands.DropBasket(basketId) =>
        ifBasketExists(basketId) { basket =>
          basket.actor ! BasketActor.Commands.ByeBye
          baskets -= basketId
          sender ! BasketDropSuccess
        }
      case ShopActor.Commands.ToBasket(basketId, cmd, forceCreate) =>
        val fn = if (forceCreate) forceWithBasket _ else ifBasketExists _
        fn(basketId) { basket =>
          basket.actor.tell(cmd, sender)
        }
      case ShopActor.Commands.GetState =>
        sender ! ShopState(baskets.keys.toList)
    }
  }

  def ifBasketExists(basketId: UUID)(func: Basket => Unit): Unit = {
    baskets.get(basketId) match {
      case Some(basketActor) => func(basketActor)
      case None => sender ! BasketNotFoundError
    }
  }

  def ifBasketNoBasket(basketId: UUID)(func: => Unit): Unit = baskets.get(basketId) match {
    case None => func
    case Some(_) => sender ! BasketAlreadyExistsError
  }

  def forceWithBasket(basketId: UUID)(func: Basket => Unit): Unit = baskets.get(basketId) match {
    case Some(basket) => func(basket)
    case None =>
      val basket = createBasketFunc(context, depot)
      baskets += basketId -> basket
      func(basket)
  }
}

object ShopActor {

  def create(ctx: ActorRefFactory, depot: Depot, createBasketFunc: (ActorRefFactory, Depot) => Basket = createBasketFunc): Shop =
    new Shop {
      override val actor = ctx.actorOf(Props(classOf[ShopActor], depot, createBasketFunc))
    }

  private def createBasketFunc(ctx: ActorRefFactory, depot: Depot): Basket =
    BasketActor.create(ctx, depot)

  sealed trait Command

  object Commands {
    case class DropBasket(basketId: UUID) extends Command
    case class ToBasket(basketId: UUID, basketActor: BasketActor.Command, forceCreate: Boolean) extends Command
    case object GetState extends Command
  }
}

trait Shop {
  val actor: ActorRef
}