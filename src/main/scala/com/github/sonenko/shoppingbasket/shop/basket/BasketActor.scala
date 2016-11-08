package com.github.sonenko.shoppingbasket.shop.basket

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, PoisonPill, Props}
import com.github.sonenko.shoppingbasket._
import com.github.sonenko.shoppingbasket.depot.{Depot, DepotActor}

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
    case _: BasketActor.Command =>
      sender ! Busy
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

    case object GetState extends Command

  }

}

trait Basket {
  val actor: ActorRef
}
