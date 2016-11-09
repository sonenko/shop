package com.github.sonenko.shoppingbasket.basketmanager.basket

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, PoisonPill, Props}
import com.github.sonenko.shoppingbasket._
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor.Commands._
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor._
import com.github.sonenko.shoppingbasket.stock.{Good, Stock, StockActor}
import org.joda.time.DateTime

class BasketActor(stock: Stock, stopSn: ActorRef => Unit) extends Actor with ActorLogging {
  var state = BasketState(Nil)

  override def receive: Receive = {
    case c: Command => c match {
      case ByeBye(putGoodsBack) =>
        beforeStop(putGoodsBack)
        stopSn(self)
      case AddGood(goodId, count) =>
        stock.actor ! StockActor.Commands.TakeGood(goodId, count)
        context.become(busy(sender))
      case GetState =>
        sender ! state
      case DropGood(goodId, count) =>
        ifGoodExistsInBasket(goodId){ goodInBasket =>
          val goodsToRemoveCount = if (goodInBasket.count > count) count else goodInBasket.count
          stock.actor ! StockActor.Commands.PutGood(goodId, goodsToRemoveCount)
          context.become(busy(sender))
        }
    }
  }

  def busy(sndr: ActorRef): Receive = {
    // FIXME this is source of problem
    // it is possible that we purchase in this moment
    case ByeBye(putGoodsBack) =>
      beforeStop(putGoodsBack)
      stopSn(self)
    case GetState =>
      sender ! state
    case res@GoodRemoveFromStockSuccess(goodFromStock) =>
      state.goods.find(_.id == goodFromStock.id) match {
        case None =>
          state = BasketState(goodFromStock :: state.goods)
        case Some(oldGood) =>
          val newGoods = state.goods.map(x =>
            if (x.id == goodFromStock.id) oldGood.copy(count = oldGood.count + goodFromStock.count)
            else x
          )
          state = BasketState(newGoods)
      }
      sndr ! AddGoodToBasketSuccess(state)
      context.unbecome()
    case res@(GoodNotFoundInStockError | GoodAmountIsLowInStockError) =>
      sndr ! res
      context.unbecome()
    case GoodAddToStockSuccess(removedGood) =>
      val goodId = removedGood.id
      val goodToRemove = state.goods.find(_.id == goodId).head
      if (goodToRemove.count == removedGood.count) {
        state = BasketState(state.goods.filter(_.id != goodId))
      } else {
        state = BasketState(state.goods.map {
          case goodInBasket @ Good(`goodId`, _, _, oldCount, _, _) => goodInBasket.copy(count = oldCount - removedGood.count)
          case x => x
        })
      }
      sndr ! RemoveGoodFromBasketSuccess(state)
      context.unbecome()
    case _: Command =>
      sender ! Busy
  }

  def ifGoodExistsInBasket(goodId: UUID)(fn: Good => Unit): Unit = state.goods.find(_.id == goodId) match {
    case None => sender ! RemoveGoodFromBasketErrorNotFountGood
    case Some(goodInBasket) => fn(goodInBasket)
  }

  def beforeStop(putGoodsBack: Boolean) = {
    if (putGoodsBack) {
      state.goods.foreach(good => {
        stock.actor ! StockActor.Commands.PutGood(good.id, good.count, false)
      })
    }
  }
}

object BasketActor {
  def create(ctx: ActorRefFactory, stock: Stock, stopSn: ActorRef => Unit = _ ! PoisonPill) = new Basket {
    override val actor = ctx.actorOf(Props(classOf[BasketActor], stock, stopSn))
  }

  sealed trait Command

  object Commands {
    case class ByeBye(putGoodsBack: Boolean) extends Command
    case class AddGood(goodId: UUID, count: Int) extends Command
    case class DropGood(goodId: UUID, count: Int) extends Command
    case object GetState extends Command
  }
}

trait Basket {
  val actor: ActorRef
  val updatedAt: DateTime = DateTime.now()
  def updated = new Basket{
    override val actor = Basket.this.actor
  }
}
