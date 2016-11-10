package com.github.sonenko.shoppingbasket.basketmanager

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.{ProductRemoveFromStockSuccess, _}
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor.Commands.ByeBye
import com.github.sonenko.shoppingbasket.stock.{Stock, StockActor}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class BasketActorTest extends TestKit(ActorSystem("BasketActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val stock = new Stock {
      override val actor = self
    }
    val product = StockActor.initialState.head
    val productId = product.id
    var stopFunctionExecuted = false

    def stopFn(act: ActorRef): Unit = {
      stopFunctionExecuted = true
      self ! BasketActor.Commands.ByeBye(false)
    }

    val basket = BasketActor.create(system, stock, stopFn)

    def addProduct(): Unit = {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basket.actor ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
    }
  }

  "BasketActor.Commands.AddProduct" should {
    "forward request to Stock" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddProduct(productId, count)
      expectMsg(StockActor.Commands.TakeProduct(productId, count))
    }
    "send request to stock and became unavailable" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.AddProduct(productId, count)
      expectMsg(StockActor.Commands.TakeProduct(productId, count))
      basket.actor ! BasketActor.Commands.AddProduct(java.util.UUID.randomUUID(), count)
      expectMsg(Busy)
    }
  }

  "BasketActor.Commands.DropProduct" should {
    "replay ProductNotFoundRemoveFromBasketError if product not in basket" in new Scope {
      val count = 1
      basket.actor ! BasketActor.Commands.DropProduct(productId, count)
      expectMsg(ProductNotFoundRemoveFromBasketError)
    }
    "send request to stock and became unavailable" in new Scope {
      addProduct()
      basket.actor ! BasketActor.Commands.DropProduct(productId, 1)
      expectMsg(StockActor.Commands.PutProduct(productId, 1))
      basket.actor ! BasketActor.Commands.DropProduct(productId, 1)
      expectMsg(Busy)
    }
    "remove all products if count more then exists" in new Scope {
      addProduct()
      basket.actor ! BasketActor.Commands.DropProduct(productId, 10)
      expectMsg(StockActor.Commands.PutProduct(productId, 1))
      basket.actor ! ProductAddToStockSuccess(product.copy(count = 1))
      expectMsg(RemoveProductFromBasketSuccess(BasketState(Nil)))
    }
  }

  "BasketActor.Commands.GetState" should {
    "replay current state" in new Scope {
      basket.actor ! BasketActor.Commands.GetState
      expectMsg(BasketState(Nil))
    }
    "replay with current state when busy" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basket.actor ! BasketActor.Commands.GetState // still busy
      expectMsg(BasketState(Nil))
    }
  }

  // busy
  "when busy BasketActor.Commands.ByeBye" should {
    "execute replay Busy" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basket.actor ! BasketActor.Commands.ByeBye(false)
      expectMsg(GotMeIWillDieAfterDielsWithStock)
      basket.actor ! BasketActor.Commands.ByeBye(false)
      expectMsg(Busy)
    }
  }
  "when busy ProductRemoveFromStockSuccess" should {
    "add product to basket if basket has not same product" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basket.actor ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
    }
    "increment product count in basket if basket has same product" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basket.actor ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
      // send iteration
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basket.actor ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket.copy(count = 2)))))
    }
  }

  "when busy ProductNotFoundInStockError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basket.actor ! ProductNotFoundInStockError
      expectMsg(ProductNotFoundInStockError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
    }
  }

  "when busy ProductAmountIsLowInStockError" should {
    "forward message end unbecome" in new Scope {
      basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 2))
      basket.actor ! ProductAmountIsLowInStockError
      expectMsg(ProductAmountIsLowInStockError)
      // check if unbecome
      basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 2))
    }
  }

  "tricky case" in new Scope {
    basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // should became busy
    expectMsg(StockActor.Commands.TakeProduct(productId, 2)) // message to Stock
    basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // trying to add more
    expectMsg(Busy) // receive Busy because we didn't receive message from stock
    basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // yes it is busy but again
    expectMsg(Busy) // busy again, ok
    basket.actor ! ByeBye(true) // lets try to kill busy basket
    expectMsg(GotMeIWillDieAfterDielsWithStock) // can not kill, but it will dye
    basket.actor ! ByeBye(true) // lets try to kill one more time
    expectMsg(Busy) // well it is busy, we can not kill basket twice, ok
    basket.actor ! BasketActor.Commands.AddProduct(productId, 2) // trying to add more
    expectMsg(Busy) // sure, it is busy
    // STOCK REPLAY
    basket.actor ! ProductRemoveFromStockSuccess(product.copy(count = 2)) // stock replay with happy case
    expectMsg(AddProductToBasketSuccess(BasketState(List(product.copy(count = 2))))) // yes before basket die it updates state and replay
    expectMsg(StockActor.Commands.PutProduct(productId, 2, false)) // put back items to stock
    expectMsg(ByeBye(false)) // executing stopFn
    expectNoMsg()
    stopFunctionExecuted = true
  }
}
