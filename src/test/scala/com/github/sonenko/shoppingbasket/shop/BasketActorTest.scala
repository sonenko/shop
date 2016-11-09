package com.github.sonenko.shoppingbasket.shop

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.stock.{Stock, StockActor}
import com.github.sonenko.shoppingbasket.shop.basket.BasketActor
import com.github.sonenko.shoppingbasket._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class BasketActorTest extends TestKit(ActorSystem("BasketActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val stock = new Stock {
      override val actor = self
    }
    val good = StockActor.initialState.head
    val goodId = good.id
    var stopFunctionExecuted = false

    def stopFn(act: ActorRef): Unit = {
      stopFunctionExecuted = true
      self ! BasketActor.Commands.ByeBye(false)
    }

    val basket = BasketActor.create(system, stock, stopFn)

    def addGood(): Unit = {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1)
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      val goodInBasket = good.copy(count = 1)
      basket.actor ! GoodRemoveFromStockSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket))))
    }
  }

  "BasketActor.Commands.AddGood" should {
    "send request to Stock" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddGood(goodId, count)
      expectMsg(StockActor.Commands.TakeGood(goodId, count))
    }
    "send request to stock and became unavailable" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddGood(goodId, count)
      expectMsg(StockActor.Commands.TakeGood(goodId, count))
      basket.actor ! BasketActor.Commands.AddGood(java.util.UUID.randomUUID(), count)
      expectMsg(Busy)
    }
  }

  "BasketActor.Commands.DropGood" should {
    "send request to Stock" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.DropGood(goodId, count)
      expectMsg(RemoveGoodFromBasketErrorNotFountGood)
    }
    "send request to stock and became unavailable" in new Scope {
      addGood()
      basket.actor ! BasketActor.Commands.DropGood(goodId, 1)
      expectMsg(StockActor.Commands.PutGood(goodId, 1))
      basket.actor ! BasketActor.Commands.DropGood(goodId, 1)
      expectMsg(Busy)
    }
    "remove all goods if count more then exists" in new Scope {
      addGood()
      basket.actor ! BasketActor.Commands.DropGood(goodId, 10)
      expectMsg(StockActor.Commands.PutGood(goodId, 1))
      basket.actor ! GoodAddToStockSuccess(good.copy(count = 1))
      expectMsg(RemoveGoodFromBasketSuccess(BasketState(Nil)))
    }
  }

  "BasketActor.Commands.GetState" should {
    "replay current state" in new Scope {
      basket.actor ! BasketActor.Commands.GetState
      expectMsg(BasketState(Nil))
    }
    "replay with current state when busy" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      basket.actor ! BasketActor.Commands.GetState // still busy
      expectMsg(BasketState(Nil))
    }
  }

  // busy
  "when busy BasketActor.Commands.ByeBye" should {
    "execute stop function" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1)
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      basket.actor ! BasketActor.Commands.ByeBye(false)
      expectMsg(BasketActor.Commands.ByeBye(false))
      stopFunctionExecuted shouldEqual true
    }
  }
  "when busy GoodRemoveFromStockSuccess" should {
    "add good to basket if basket has not same good" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1)
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      val goodInBasket = good.copy(count = 1)
      basket.actor ! GoodRemoveFromStockSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket))))
    }
    "increment good count in basket if basket has same good" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      val goodInBasket = good.copy(count = 1)
      basket.actor ! GoodRemoveFromStockSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket))))
      // send iteration
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      basket.actor ! GoodRemoveFromStockSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket.copy(count = 2)))))
    }
  }

  "when busy GoodNotFoundInStockError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
      basket.actor ! GoodNotFoundInStockError
      expectMsg(GoodNotFoundInStockError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 1))
    }
  }

  "when busy GoodAmountIsLowInStockError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 2))
      basket.actor ! GoodAmountIsLowInStockError
      expectMsg(GoodAmountIsLowInStockError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddGood(goodId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeGood(goodId, 2))
    }
  }
}
