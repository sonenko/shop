package com.github.sonenko.shoppingbasket.shop

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.depot.{Depot, DepotActor}
import com.github.sonenko.shoppingbasket.shop.basket.BasketActor
import com.github.sonenko.shoppingbasket.{AddGoodToBasketSuccess, BasketState, Busy, GoodAmountIsLowInDepotError, GoodNotFoundInDepotError, GoodRemoveFromDepotSuccess}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class BasketActorTest extends TestKit(ActorSystem("BasketActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val depot = new Depot {
      override val actor = self
    }
    val good = DepotActor.initialState.head
    val goodId = good.id
    var stopFunctionExecuted = false
    def stopFn(act: ActorRef): Unit = {
      stopFunctionExecuted = true
      self ! BasketActor.Commands.ByeBye
    }
    val basket = BasketActor.create(system, depot, stopFn)
  }

  "BasketActor.Commands.AddGood" should {
    "send request to Depot" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddGood(goodId, count)
      expectMsg(DepotActor.Commands.TakeGood(goodId, count))
    }
    "send request to depot and became unavailable" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddGood(goodId, count)
      expectMsg(DepotActor.Commands.TakeGood(goodId, count))
      basket.actor ! BasketActor.Commands.AddGood(java.util.UUID.randomUUID(), count)
      expectMsg(Busy)
    }
  }
  "when busy BasketActor.Commands.ByeBye" should {
    "execute stop function" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1)
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
      basket.actor ! BasketActor.Commands.ByeBye
      expectMsg(BasketActor.Commands.ByeBye)
      stopFunctionExecuted shouldEqual true
    }
  }
  "when busy GoodRemoveFromDepotSuccess" should {
    "add good to basket if basket has not same good" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1)
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
      val goodInBasket = good.copy(count = 1)
      basket.actor ! GoodRemoveFromDepotSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket))))
    }
    "increment good count in basket if basket has same good" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
      val goodInBasket = good.copy(count = 1)
      basket.actor ! GoodRemoveFromDepotSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket))))
      // send iteration
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
      basket.actor ! GoodRemoveFromDepotSuccess(goodInBasket)
      expectMsg(AddGoodToBasketSuccess(BasketState(List(goodInBasket.copy(count = 2)))))
    }
  }

  "when busy GoodNotFoundInDepotError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
      basket.actor ! GoodNotFoundInDepotError
      expectMsg(GoodNotFoundInDepotError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddGood(goodId, 1) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 1))
    }
  }

  "when busy GoodAmountIsLowInDepotError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddGood(goodId, 2) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 2))
      basket.actor ! GoodAmountIsLowInDepotError
      expectMsg(GoodAmountIsLowInDepotError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddGood(goodId, 2) // to became busy
      expectMsg(DepotActor.Commands.TakeGood(goodId, 2))
    }
  }
}