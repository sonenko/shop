//package com.github.sonenko.shoppingbasket.basketmanager
//
//
//import akka.actor.{ActorRefFactory, ActorSystem}
//import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
//import com.github.sonenko.shoppingbasket.basketmanager.basket.{Basket, BasketActor}
//import com.github.sonenko.shoppingbasket.stock.Stock
//import com.github.sonenko.shoppingbasket.{BasketDropSuccess, BasketManagerState, BasketNotFoundError}
//import org.scalatest.mock.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
//
//class BasketManagerActorTest extends TestKit(ActorSystem("BasketManagerActorTest")) with WordSpecLike with Matchers
//  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {
//
//  trait Scope {
//    val basketId = java.util.UUID.randomUUID()
//    val stock = mock[Stock]
//    var basketCreated = false
//
//    def createFakeBasketFunc(ctx: ActorRefFactory, stock: Stock): Basket = {
//      basketCreated = true
//      new Basket {
//        override val actor = self
//      }
//    }
//
//    val basketManager = BasketManagerActor.create(system, stock, createFakeBasketFunc)
//  }
//
//  "BasketManagerActor.Commands.GetState" should {
//    "respond with current state(empty)" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.GetState
//      expectMsg(BasketManagerState(Nil))
//    }
//    "respond with current state(one basket)" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
//      expectMsg(BasketActor.Commands.ByeBye(false))
//      basketManager.actor ! BasketManagerActor.Commands.GetState
//      expectMsg(BasketManagerState(List(basketId)))
//    }
//  }
//
//  "BasketManagerActor.Commands.DropBasket" should {
//    "respond with BasketNotFoundError if there is no basket with specified ID" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.DropBasket(basketId)
//      expectMsg(BasketNotFoundError)
//    }
//    "send ByeBye to Basket and respond with BasketDropSuccess and remove actor from basket" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
//      expectMsg(BasketActor.Commands.ByeBye(false))
//      basketManager.actor ! BasketManagerActor.Commands.DropBasket(basketId)
//      expectMsg(BasketActor.Commands.ByeBye(true))
//      expectMsg(BasketDropSuccess)
//      basketManager.actor ! BasketManagerActor.Commands.GetState
//      expectMsg(BasketManagerState(Nil))
//    }
//  }
//
//  "BasketManagerActor.Commands.ToBasket" should {
//    "respond BasketNotFoundError if the is no basket with specified ID" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.ToBasket(java.util.UUID.randomUUID(), BasketActor.Commands.ByeBye(false), false)
//      expectMsg(BasketNotFoundError)
//    }
//    "forward message to basket" in new Scope {
//      basketManager.actor ! BasketManagerActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
//      expectMsg(BasketActor.Commands.ByeBye(false))
//    }
//  }
//}
