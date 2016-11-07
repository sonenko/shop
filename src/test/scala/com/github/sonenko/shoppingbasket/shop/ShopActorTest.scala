package com.github.sonenko.shoppingbasket.shop


import akka.actor.{ActorContext, ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.depot.Depot
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ShopActorTest extends TestKit(ActorSystem("StoreTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
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
      shop ! ShopActor.Commands.CreateBasket
      expectMsgClass(classOf[ShopActor.Answers.BasketCreateSuccess])
      basketCreated shouldEqual true
    }
  }
}
