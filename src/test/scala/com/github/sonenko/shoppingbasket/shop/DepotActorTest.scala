package com.github.sonenko.shoppingbasket.shop

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.sonenko.shoppingbasket.depot.DepotActor
import com.github.sonenko.shoppingbasket.{DepotState, GoodAddToDepotSuccess, GoodAmountIsLowInDepotError, GoodNotFoundInDepotError, GoodRemoveFromDepotSuccess}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DepotActorTest extends TestKit(ActorSystem("DepotActorTest")) with WordSpecLike with Matchers
  with MockitoSugar with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  trait Scope {
    val depotActor = DepotActor.create(system).actor
    val initialState = DepotState(DepotActor.initialState)
    val initialCount = initialState.goods.head.count
    val good = initialState.goods.head.copy(count = 1)
    val goodId = good.id
  }

  "DepotActor.Commands.GetState" should {
    "send back actor current state" in new Scope {
      depotActor ! DepotActor.Commands.GetState
      expectMsg(initialState)
    }
  }

  "DepotActor.Commands.TakeGood" should {
    "replay GoodRemoveFromDepotSuccess if good exists and remove good" in new Scope {
      val count = 2
      depotActor ! DepotActor.Commands.TakeGood(goodId, count)
      expectMsg(GoodRemoveFromDepotSuccess(good.copy(count = 2)))
      depotActor ! DepotActor.Commands.GetState
      expectMsgPF() {
        case DepotState(goods) if goods.find(_.id == goodId).head.count == initialCount - count => ()
      }
    }
    "replay GoodNotFoundInDepotError if depot does not contain good with specified Id" in new Scope {
      depotActor ! DepotActor.Commands.TakeGood(java.util.UUID.randomUUID(), 1)
      expectMsg(GoodNotFoundInDepotError)
    }
    "replay GoodAmountIsLowInDepotError if depot does not has so alot of goods with specified Id" in new Scope {
      depotActor ! DepotActor.Commands.TakeGood(goodId, 50)
      expectMsg(GoodAmountIsLowInDepotError)
    }
  }

  "DepotActor.Commands.PutGood" should {
    "replay GoodAddToDepotSuccess in happy case" in new Scope { // yes only specified goods for now
      depotActor ! DepotActor.Commands.PutGood(goodId, 3)
      expectMsg(GoodAddToDepotSuccess(good.copy(count = 3)))
      depotActor ! DepotActor.Commands.GetState
      expectMsgPF() {
        case DepotState(goods) if goods.find(_.id == goodId).head.count == initialCount + 3 => ()
      }
    }
    "replay GoodNotFoundInDepotError if depot does not contain good with specified Id" in new Scope {
      depotActor ! DepotActor.Commands.PutGood(java.util.UUID.randomUUID(), 1)
      expectMsg(GoodNotFoundInDepotError)
    }
  }
}
