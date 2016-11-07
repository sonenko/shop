package com.github.sonenko.shoppingbasket.shop

import java.util.UUID

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.basket.BasketActor

class ShopActor(depot: Depot, createBasketFunc: (ActorContext, Depot) => ActorRef) extends Actor {
  var baskets: Map[UUID, ActorRef] = Map()

  override def receive: Receive = {
    case cmd: ShopActor.Command => cmd match {
      case ShopActor.Commands.CreateBasket =>
        val basketId = java.util.UUID.randomUUID()
        val basket = createBasketFunc(context, depot)
        baskets = baskets + (basketId -> basket)
        sender ! ShopActor.Answers.BasketCreateSuccess(basketId)
    }
  }
}


object ShopActor {

  private def createBasketFunc(ctx: ActorContext, depot: Depot): ActorRef =
    ctx.actorOf(BasketActor.props(depot))

  def props(depot: Depot, createBasketFunc: (ActorContext, Depot) => ActorRef = createBasketFunc) =
    Props(classOf[ShopActor], depot, createBasketFunc)


  sealed trait Command
  object Commands {
    case object CreateBasket extends Command
  }

  sealed trait Answer
  object Answers {
    case class BasketCreateSuccess(basketId: UUID) extends Answer
  }
}