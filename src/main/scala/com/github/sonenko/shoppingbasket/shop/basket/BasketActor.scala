package com.github.sonenko.shoppingbasket.shop.basket

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, PoisonPill, Props}
import com.github.sonenko.shoppingbasket._
import com.github.sonenko.shoppingbasket.depot.{Depot, DepotActor, Good}

class BasketActor(depot: Depot, stopSn: ActorRef => Unit) extends Actor with ActorLogging {
  var state = BasketState(Nil)

  override def receive: Receive = {
    case c: BasketActor.Command => c match {
      case BasketActor.Commands.ByeBye =>
        beforeStop()
        stopSn(self)
      case BasketActor.Commands.AddGood(goodId, count) =>
        depot.actor ! DepotActor.Commands.TakeGood(goodId, count)
        context.become(busy(sender))
      case BasketActor.Commands.GetState =>
        sender ! state
      case BasketActor.Commands.DropGood(goodId, count) =>
        ifGoodExistsInBasket(goodId){ goodInBasket =>
          val goodsToRemoveCount = if (goodInBasket.count > count) goodInBasket.count - count else goodInBasket.count
          depot.actor ! DepotActor.Commands.PutGood(goodId, goodsToRemoveCount)
          context.become(busy(sender))
        }
    }
  }

  def busy(sndr: ActorRef): Receive = {
    case BasketActor.Commands.ByeBye =>
      beforeStop()
      stopSn(self)
    case BasketActor.Commands.GetState =>
      sender ! state
    case res@GoodRemoveFromDepotSuccess(goodFromDepot) =>
      state.goods.find(_.id == goodFromDepot.id) match {
        case None =>
          state = BasketState(goodFromDepot :: state.goods)
        case Some(oldGood) =>
          val newGoods = state.goods.map(x =>
            if (x.id == goodFromDepot.id) oldGood.copy(count = oldGood.count + goodFromDepot.count)
            else x
          )
          state = BasketState(newGoods)
      }
      sndr ! AddGoodToBasketSuccess(state)
      context.unbecome()
    case res@(GoodNotFoundInDepotError | GoodAmountIsLowInDepotError) =>
      sndr ! res
      context.unbecome()
    case GoodAddToDepotSuccess(removedGood) =>
      val goodId = removedGood.id
      val goodToRemove = state.goods.find(_.id == goodId).head
      if (goodToRemove.count == removedGood.count) {
        state = BasketState(state.goods.filter(_.id != goodId))
      } else {
        state = BasketState(state.goods.map {
          case goodInBasket @ Good(`goodId`, _, _, oldCount, _) => goodInBasket.copy(count = oldCount - removedGood.count)
          case x => x
        })
      }
      sndr ! RemoveGoodFromBasketSuccess(state)
      context.unbecome()
    case _: BasketActor.Command =>
      sender ! Busy
  }

  def ifGoodExistsInBasket(goodId: UUID)(fn: Good => Unit): Unit = state.goods.find(_.id == goodId) match {
    case None => sender ! RemoveGoodFromBasketErrorNotFountGood
    case Some(goodInBasket) => fn(goodInBasket)
  }


  def beforeStop() = {
    log.error("Implement me, put goods back to depot")
  }
}

object BasketActor {
  def create(ctx: ActorRefFactory, depot: Depot, stopSn: ActorRef => Unit = _ ! PoisonPill) = new Basket {
    override val actor = ctx.actorOf(Props(classOf[BasketActor], depot, stopSn))
  }

  sealed trait Command

  object Commands {
    case object ByeBye extends Command
    case class AddGood(goodId: UUID, count: Int) extends Command
    case class DropGood(goodId: UUID, count: Int) extends Command
    case object GetState extends Command
  }

}

trait Basket {
  val actor: ActorRef
}
