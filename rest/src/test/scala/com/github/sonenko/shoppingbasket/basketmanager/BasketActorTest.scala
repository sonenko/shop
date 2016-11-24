package com.github.sonenko.shoppingbasket.basketmanager

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor.Commands.ByeBye
import com.github.sonenko.shoppingbasket.stock.{Stock, StockActor}
import com.github.sonenko.shoppingbasket.{ProductRemoveFromStockSuccess, _}
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

    val basketActorRef = TestActorRef(new BasketActor(stock, stopFn))
    val basketActor = basketActorRef.underlyingActor

    def addProduct(): Unit = {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basketActorRef ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
    }
  }

    "BasketActor.Commands.GetState" should {
      "replay current state (empty)" in new Scope {
        basketActorRef ! BasketActor.Commands.GetState
        expectMsg(BasketState(Nil))
      }
      "replay current state (non-empty)" in new Scope {
        basketActor.state = BasketState(List(StockActor.initialState.head))
        basketActorRef ! BasketActor.Commands.GetState
        expectMsg(basketActor.state)
      }
      "replay with current state when busy" in new Scope {
        basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
        expectMsg(StockActor.Commands.TakeProduct(productId, 1))
        basketActorRef ! BasketActor.Commands.GetState // still busy
        expectMsg(BasketState(Nil))
      }
      "replay with current state when busy and prepared to die" in new Scope {
        basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
        expectMsg(StockActor.Commands.TakeProduct(productId, 1))
        basketActorRef ! BasketActor.Commands.ByeBye(true) // now dying
        expectMsg(GotMeIWillDieAfterDielsWithStock)
        basketActorRef ! BasketActor.Commands.GetState // still busy
        expectMsg(BasketState(Nil))
      }
    }

  "BasketActor.Commands.AddProduct" should {
    "forward request to Stock" in new Scope {
      val count = 1
      basketActorRef ! BasketActor.Commands.AddProduct(productId, count)
      expectMsg(StockActor.Commands.TakeProduct(productId, count))
    }
    "send request to stock and became unavailable" in new Scope {
      val count = 1
      basketActorRef ! BasketActor.Commands.AddProduct(productId, count)
      expectMsg(StockActor.Commands.TakeProduct(productId, count))
      basketActorRef ! BasketActor.Commands.AddProduct(java.util.UUID.randomUUID(), count)
      expectMsg(Busy)
    }
    "add products to state if Stock replied success" in new Scope {
      val count = 1
      basketActorRef ! BasketActor.Commands.AddProduct(productId, count)
      expectMsg(StockActor.Commands.TakeProduct(productId, count))
      basketActorRef ! ProductRemoveFromStockSuccess(product.copy(count = count))
      expectMsg(AddProductToBasketSuccess(basketActor.state))
      basketActor.state shouldEqual BasketState(List(product.copy(count = 1)))
    }
  }

  "BasketActor.Commands.DropProduct" should {
    "replay ProductNotFoundRemoveFromBasketError if product not in basket" in new Scope {
      val count = 1
      basketActorRef ! BasketActor.Commands.DropProduct(productId, count)
      expectMsg(ProductNotFoundRemoveFromBasketError)
    }
    "send request to stock and became unavailable" in new Scope {
      addProduct()
      basketActorRef ! BasketActor.Commands.DropProduct(productId, 1)
      expectMsg(StockActor.Commands.PutProduct(productId, 1))
      basketActorRef ! BasketActor.Commands.DropProduct(productId, 1)
      expectMsg(Busy)
    }
    "remove all products if count more then exists" in new Scope {
      addProduct()
      basketActorRef ! BasketActor.Commands.DropProduct(productId, 10)
      expectMsg(StockActor.Commands.PutProduct(productId, 1))
      basketActorRef ! ProductAddToStockSuccess(product.copy(count = 1))
      expectMsg(RemoveProductFromBasketSuccess(BasketState(Nil)))
      basketActor.state.products shouldEqual Nil
    }
  }

  // busy
  "when busy BasketActor.Commands.ByeBye" should {
    "execute replay Busy" in new Scope {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basketActorRef ! BasketActor.Commands.ByeBye(false)
      expectMsg(GotMeIWillDieAfterDielsWithStock)
      basketActorRef ! BasketActor.Commands.ByeBye(false)
      expectMsg(Busy)
    }
  }
  "when busy ProductRemoveFromStockSuccess" should {
    "add product to basket if basket has not same product" in new Scope {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1)
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basketActorRef ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
    }
    "increment product count in basket if basket has same product" in new Scope {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      val productInBasket = product.copy(count = 1)
      basketActorRef ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket))))
      // send iteration
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basketActorRef ! ProductRemoveFromStockSuccess(productInBasket)
      expectMsg(AddProductToBasketSuccess(BasketState(List(productInBasket.copy(count = 2)))))
    }
  }

  "when busy ProductNotFoundInStockError" should {
    "forward message end unbecome" in new Scope {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
      basketActorRef ! ProductNotFoundInStockError
      expectMsg(ProductNotFoundInStockError)
      // check if unbecome
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 1) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 1))
    }
  }

  "when busy ProductAmountIsLowInStockError" should {
    "forward message end unbecome" in new Scope {
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 2))
      basketActorRef ! ProductAmountIsLowInStockError
      expectMsg(ProductAmountIsLowInStockError)
      // check if unbecome
      basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // to became busy
      expectMsg(StockActor.Commands.TakeProduct(productId, 2))
    }
  }

  "tricky case" in new Scope {
    basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // should became busy
    expectMsg(StockActor.Commands.TakeProduct(productId, 2)) // message to Stock
    basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // trying to add more
    expectMsg(Busy) // receive Busy because we didn't receive message from stock
    basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // yes it is busy but again
    expectMsg(Busy) // busy again, ok
    basketActorRef ! ByeBye(true) // lets try to kill busy basket
    expectMsg(GotMeIWillDieAfterDielsWithStock) // can not kill, but it will dye
    basketActorRef ! ByeBye(true) // lets try to kill one more time
    expectMsg(Busy) // well it is busy, we can not kill basket twice, ok
    basketActorRef ! BasketActor.Commands.AddProduct(productId, 2) // trying to add more
    expectMsg(Busy) // sure, it is busy
    // STOCK REPLAY
    basketActorRef ! ProductRemoveFromStockSuccess(product.copy(count = 2)) // stock replay with happy case
    expectMsg(AddProductToBasketSuccess(BasketState(List(product.copy(count = 2))))) // yes before basket die it updates state and replay
    expectMsg(StockActor.Commands.PutProduct(productId, 2, false)) // put back items to stock
    expectMsg(ByeBye(false)) // executing stopFn
    expectNoMsg()
    stopFunctionExecuted = true
  }
}
