//package com.github.sonenko.shoppingbasket.basketmanager
//
//import akka.actor.ActorSystem
//import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
//import com.github.sonenko.shoppingbasket.stock.StockActor
//import com.github.sonenko.shoppingbasket.{ProductAddToStockSuccess, ProductAmountIsLowInStockError, ProductNotFoundInStockError, ProductRemoveFromStockSuccess, StockState}
//import org.scalatest.mock.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
//
//class StockActorTest extends TestKit(ActorSystem("StockActorTest")) with WordSpecLike with Matchers
//  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {
//
//  trait Scope {
//    val stockActor = StockActor.create(system).actor
//    val initialState = StockState(StockActor.initialState)
//    val initialCount = initialState.products.head.count
//    val product = initialState.products.head.copy(count = 1)
//    val productId = product.id
//  }
//
//  "StockActor.Commands.GetState" should {
//    "send back actor current state" in new Scope {
//      stockActor ! StockActor.Commands.GetState
//      expectMsg(initialState)
//    }
//  }
//
//  "StockActor.Commands.TakeProduct" should {
//    "replay ProductRemoveFromStockSuccess if product exists and remove product" in new Scope {
//      val count = 2
//      stockActor ! StockActor.Commands.TakeProduct(productId, count)
//      expectMsg(ProductRemoveFromStockSuccess(product.copy(count = 2)))
//      stockActor ! StockActor.Commands.GetState
//      expectMsgPF() {
//        case StockState(products) if products.find(_.id == productId).head.count == initialCount - count => ()
//      }
//    }
//    "replay ProductNotFoundInStockError if stock does not contain product with specified Id" in new Scope {
//      stockActor ! StockActor.Commands.TakeProduct(java.util.UUID.randomUUID(), 1)
//      expectMsg(ProductNotFoundInStockError)
//    }
//    "replay ProductAmountIsLowInStockError if stock does not has so alot of productss with specified Id" in new Scope {
//      stockActor ! StockActor.Commands.TakeProduct(productId, 50)
//      expectMsg(ProductAmountIsLowInStockError)
//    }
//  }
//
//  "StockActor.Commands.PutProduct" should {
//    "replay ProductAddToStockSuccess in happy case" in new Scope { // yes only specified products for now
//      stockActor ! StockActor.Commands.PutProduct(productId, 3)
//      expectMsg(ProductAddToStockSuccess(product.copy(count = 3)))
//      stockActor ! StockActor.Commands.GetState
//      expectMsgPF() {
//        case StockState(products) if products.find(_.id == productId).head.count == initialCount + 3 => ()
//      }
//    }
//    "replay ProductNotFoundInStockError if stock does not contain product with specified Id" in new Scope {
//      stockActor ! StockActor.Commands.PutProduct(java.util.UUID.randomUUID(), 1)
//      expectMsg(ProductNotFoundInStockError)
//    }
//  }
//}
