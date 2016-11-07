package com.github.sonenko.shoppingbasket.shop

import java.util.UUID

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.basket.BasketActor

class ShopActor(depot: Depot, createBasketFunc: (ActorContext, Depot) => ActorRef) extends Actor {
  var baskets: Map[UUID, ActorRef] = Map()

  override def receive: Receive = {
    case c: ShopActor.Command => c match {
      case ShopActor.Commands.CreateBasket(basketId) =>
        val basket = createBasketFunc(context, depot)
        baskets = baskets + (basketId -> basket)
        sender ! ShopActor.Answers.BasketCreateSuccess(basketId)
    }
    case q: ShopActor.Query => q match {
      case ShopActor.Queries.GetState =>
        sender ! ShopActor.Answers.State(baskets.keys.toList)
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
    case class CreateBasket(basketId: UUID) extends Command
  }

  sealed trait Query
  object Queries {
    case object GetState extends Query
  }

  sealed trait Answer
  object Answers {
    case class BasketCreateSuccess(basketId: UUID) extends Answer
    case class State(basketIds: List[UUID]) extends Answer
  }
}