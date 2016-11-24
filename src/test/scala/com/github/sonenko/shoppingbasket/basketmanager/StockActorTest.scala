package com.github.sonenko.shoppingbasket.basketmanager

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.github.sonenko.shoppingbasket._
import com.github.sonenko.shoppingbasket.stock.StockActor
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class StockActorTest extends TestKit(ActorSystem("StockActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait Scope {
    val stockActor = TestActorRef(new StockActor())
    val initialState = StockState(StockActor.initialState)
    val initialCount = initialState.products.head.count
    val product = initialState.products.head.copy(count = 1)
    val productId = product.id
  }

  "StockActor.Commands.GetState" should {
    "send back actor current state" in new Scope {
      stockActor ! StockActor.Commands.GetState
      expectMsg(initialState)
    }
  }

  "StockActor.Commands.TakeProduct" should {
    "replay ProductRemoveFromStockSuccess if product exists and remove product" in new Scope {
      val count = 2
      stockActor ! StockActor.Commands.TakeProduct(productId, count)
      expectMsg(ProductRemoveFromStockSuccess(product.copy(count = count))) // return product removed state
      stockActor.underlyingActor.state.head.count shouldEqual (initialCount - count) // state should change
    }
    "replay ProductNotFoundInStockError if stock does not contain product with specified Id" in new Scope {
      stockActor ! StockActor.Commands.TakeProduct(java.util.UUID.randomUUID(), 1)
      expectMsg(ProductNotFoundInStockError)
      stockActor.underlyingActor.state shouldEqual initialState.products // should not affect state
    }
    "replay ProductAmountIsLowInStockError if stock does not has so alot of products with specified Id" in new Scope {
      stockActor ! StockActor.Commands.TakeProduct(productId, 50)
      expectMsg(ProductAmountIsLowInStockError)
      stockActor.underlyingActor.state shouldEqual initialState.products // should not affect state
    }
  }

  "StockActor.Commands.PutProduct" should {
    "replay ProductAddToStockSuccess in happy case" in new Scope { // yes only specified products for now
      val toAddCount = 3
      stockActor ! StockActor.Commands.PutProduct(productId, toAddCount)
      expectMsg(ProductAddToStockSuccess(product.copy(count = toAddCount)))
      stockActor.underlyingActor.state.head.count shouldEqual (initialCount + toAddCount) // state should be changed
    }
    "replay ProductNotFoundInStockError if stock does not contain product with specified Id" in new Scope {
      stockActor ! StockActor.Commands.PutProduct(java.util.UUID.randomUUID(), 1)
      expectMsg(ProductNotFoundInStockError)
      stockActor.underlyingActor.state.head.count shouldEqual initialCount // should not affect state
    }
  }
}
