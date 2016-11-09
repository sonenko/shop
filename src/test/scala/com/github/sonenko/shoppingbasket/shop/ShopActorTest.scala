package com.github.sonenko.shoppingbasket.shop


import akka.actor.{ActorRefFactory, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.basket.{Basket, BasketActor}
import com.github.sonenko.shoppingbasket.{BasketDropSuccess, BasketNotFoundError, ShopState}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ShopActorTest extends TestKit(ActorSystem("ShopActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val basketId = java.util.UUID.randomUUID()
    val depot = mock[Depot]
    var basketCreated = false

    def createFakeBasketFunc(ctx: ActorRefFactory, depot: Depot): Basket = {
      basketCreated = true
      new Basket {
        override val actor = self
      }
    }

    val shop = ShopActor.create(system, depot, createFakeBasketFunc)
  }

  "ShopActor.Commands.GetState" should {
    "respond with current state(empty)" in new Scope {
      shop.actor ! ShopActor.Commands.GetState
      expectMsg(ShopState(Nil))
    }
    "respond with current state(one basket)" in new Scope {
      shop.actor ! ShopActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
      expectMsg(BasketActor.Commands.ByeBye(false))
      shop.actor ! ShopActor.Commands.GetState
      expectMsg(ShopState(List(basketId)))
    }
  }

  "ShopActor.Commands.DropBasket" should {
    "respond with BasketNotFoundError if there is no basket with specified ID" in new Scope {
      shop.actor ! ShopActor.Commands.DropBasket(basketId)
      expectMsg(BasketNotFoundError)
    }
    "send ByeBye to Basket and respond with ShopActor.Answers.BasketDropSuccess and remove actor from basket" in new Scope {
      shop.actor ! ShopActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
      expectMsg(BasketActor.Commands.ByeBye(false))
      shop.actor ! ShopActor.Commands.DropBasket(basketId)
      expectMsg(BasketActor.Commands.ByeBye(false))
      expectMsg(BasketDropSuccess)
      shop.actor ! ShopActor.Commands.GetState
      expectMsg(ShopState(Nil))
    }
  }

  "ShopActor.Commands.ToBasket" should {
    "respond BasketNotFoundError if the is no basket with specified ID" in new Scope {
      shop.actor ! ShopActor.Commands.ToBasket(java.util.UUID.randomUUID(), BasketActor.Commands.ByeBye(false), false)
      expectMsg(BasketNotFoundError)
    }
    "forward message to basket" in new Scope {
      shop.actor ! ShopActor.Commands.ToBasket(basketId, BasketActor.Commands.ByeBye(false), true) // create basket implicitly
      expectMsg(BasketActor.Commands.ByeBye(false))
    }
  }
}
