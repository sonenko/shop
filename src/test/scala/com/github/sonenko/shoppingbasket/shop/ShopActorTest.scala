package com.github.sonenko.shoppingbasket.shop


import akka.actor.{ActorContext, ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.depot.Depot
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ShopActorTest extends TestKit(ActorSystem("StoreTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val basketId = java.util.UUID.randomUUID()
    val depot = mock[Depot]
    var basketCreated = false
    def createFakeBasketFunc(ctx: ActorContext, depot: Depot): ActorRef = {
      basketCreated = true
      self
    }
    val shop = system.actorOf(ShopActor.props(depot, createFakeBasketFunc))
  }

  "ShopActor.Commands.CreateBasket" should {
    "execute createBasketFunc and respond with newly created basketId" in new Scope {
      shop ! ShopActor.Commands.CreateBasket(basketId)
      expectMsg(ShopActor.Answers.BasketCreateSuccess(basketId))
      basketCreated shouldEqual true
    }
  }

  "ShopActor.Queries.GetState" should {
    "respond with current state(empty)" in new Scope {
      shop ! ShopActor.Queries.GetState
      expectMsg(ShopActor.Answers.State(Nil))
    }
    "respond with current state(one basket)" in new Scope {
      shop ! ShopActor.Commands.CreateBasket(basketId)
      expectMsg(ShopActor.Answers.BasketCreateSuccess(basketId))
      shop ! ShopActor.Queries.GetState
      expectMsg(ShopActor.Answers.State(List(basketId)))
    }
  }
}
