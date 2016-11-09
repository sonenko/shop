package com.github.sonenko.shoppingbasket.basketmanager

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{GoodAddToStockSuccess, GoodAmountIsLowInStockError, GoodNotFoundInStockError, GoodRemoveFromStockSuccess, StockState}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class StockActorTest extends TestKit(ActorSystem("StockActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val stockActor = StockActor.create(system).actor
    val initialState = StockState(StockActor.initialState)
    val initialCount = initialState.goods.head.count
    val good = initialState.goods.head.copy(count = 1)
    val goodId = good.id
  }

  "StockActor.Commands.GetState" should {
    "send back actor current state" in new Scope {
      stockActor ! StockActor.Commands.GetState
      expectMsg(initialState)
    }
  }

  "StockActor.Commands.TakeGood" should {
    "replay GoodRemoveFromStockSuccess if good exists and remove good" in new Scope {
      val count = 2
      stockActor ! StockActor.Commands.TakeGood(goodId, count)
      expectMsg(GoodRemoveFromStockSuccess(good.copy(count = 2)))
      stockActor ! StockActor.Commands.GetState
      expectMsgPF() {
        case StockState(goods) if goods.find(_.id == goodId).head.count == initialCount - count => ()
      }
    }
    "replay GoodNotFoundInStockError if stock does not contain good with specified Id" in new Scope {
      stockActor ! StockActor.Commands.TakeGood(java.util.UUID.randomUUID(), 1)
      expectMsg(GoodNotFoundInStockError)
    }
    "replay GoodAmountIsLowInStockError if stock does not has so alot of goods with specified Id" in new Scope {
      stockActor ! StockActor.Commands.TakeGood(goodId, 50)
      expectMsg(GoodAmountIsLowInStockError)
    }
  }

  "StockActor.Commands.PutGood" should {
    "replay GoodAddToStockSuccess in happy case" in new Scope { // yes only specified goods for now
      stockActor ! StockActor.Commands.PutGood(goodId, 3)
      expectMsg(GoodAddToStockSuccess(good.copy(count = 3)))
      stockActor ! StockActor.Commands.GetState
      expectMsgPF() {
        case StockState(goods) if goods.find(_.id == goodId).head.count == initialCount + 3 => ()
      }
    }
    "replay GoodNotFoundInStockError if stock does not contain good with specified Id" in new Scope {
      stockActor ! StockActor.Commands.PutGood(java.util.UUID.randomUUID(), 1)
      expectMsg(GoodNotFoundInStockError)
    }
  }
}
