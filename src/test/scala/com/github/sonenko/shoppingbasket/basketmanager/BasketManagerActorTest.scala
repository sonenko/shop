package com.github.sonenko.shoppingbasket.basketmanager


import java.util.UUID

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.github.sonenko.shoppingbasket.basketmanager.basket.{Basket, BasketActor}
import com.github.sonenko.shoppingbasket.stock.{Stock, StockActor}
import com.github.sonenko.shoppingbasket.{BasketDropSuccess, BasketManagerState, BasketNotFoundError}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class BasketManagerActorTest extends TestKit(ActorSystem("BasketManagerActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait Scope {
    val basketId = java.util.UUID.randomUUID()
    val stock = mock[Stock]
    var createFunctionExecuted = false

    def createFakeBasketFunc(ctx: ActorRefFactory, stock: Stock): Basket = {
      createFunctionExecuted = true
      new Basket {
        override val actor = self
      }
    }
    val basketManagerActorRef = TestActorRef(new BasketManagerActor(stock, createFakeBasketFunc))
  }

  "BasketManagerActor.Commands.GetState" should {
    "respond with current state(empty)" in new Scope {
      // check initial state
      basketManagerActorRef.underlyingActor.baskets shouldEqual Map()
      // check state using actors API
      basketManagerActorRef ! BasketManagerActor.Commands.GetState
      expectMsg(BasketManagerState(Nil))
    }
    "respond with current state(one basket)" in new Scope {
      createBasket(basketManagerActorRef, basketId)
      basketManagerActorRef ! BasketManagerActor.Commands.GetState
      expectMsg(BasketManagerState(List(basketId)))
    }
  }

  "BasketManagerActor.Commands.DropBasket" should {
    "respond with BasketNotFoundError if there is no basket with specified ID" in new Scope {
      basketManagerActorRef ! BasketManagerActor.Commands.DropBasket(basketId)
      expectMsg(BasketNotFoundError)
    }
    "send ByeBye to Basket and respond with BasketDropSuccess and remove actor from basket" in new Scope {
      createBasket(basketManagerActorRef, basketId)
      basketManagerActorRef ! BasketManagerActor.Commands.DropBasket(basketId)
      expectMsg(BasketActor.Commands.ByeBye(true))
      expectMsg(BasketDropSuccess)
      basketManagerActorRef.underlyingActor.baskets shouldEqual Map()
    }
  }

  "BasketManagerActor.Commands.ToBasket" should {
    "respond BasketNotFoundError if the is no basket with specified ID" in new Scope {
      basketManagerActorRef ! BasketManagerActor.Commands.ToBasket(java.util.UUID.randomUUID(), BasketActor.Commands.ByeBye(false), false)
      expectMsg(BasketNotFoundError)
      basketManagerActorRef.underlyingActor.baskets shouldEqual Map()
    }
    "create new basket and forward message" in new Scope {
      basketManagerActorRef ! BasketManagerActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
      expectMsg(BasketActor.Commands.ByeBye(false))
      basketManagerActorRef.underlyingActor.baskets.size shouldEqual 1
    }
  }

  "BasketManagerActor.Commands.ExpireBaskets" should {
    "receive this message every {Config.expireBasketsEverySeconds}, it will send ByeBye to BasketActors, and remove deprecated BasketActors" in new Scope {
      createBasket(basketManagerActorRef, UUID.randomUUID())
      Thread.sleep(1500)
      expectMsg(BasketActor.Commands.ByeBye(true))
      basketManagerActorRef.underlyingActor.baskets.size shouldEqual 0
    }
  }

  def createBasket(basketManagerActorRef: ActorRef, basketId: UUID): Unit = {
    val addProdMsg = BasketActor.Commands.AddProduct(StockActor.initialState.head.id, 1)
    basketManagerActorRef ! BasketManagerActor.Commands.ToBasket(basketId, addProdMsg, true)
    expectMsg(addProdMsg)
  }
}
