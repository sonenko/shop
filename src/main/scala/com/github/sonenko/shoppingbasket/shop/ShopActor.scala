package com.github.sonenko.shoppingbasket
package shop

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.ShopActor.Commands.{ExpireBaskets, _}
import com.github.sonenko.shoppingbasket.shop.ShopActor._
import com.github.sonenko.shoppingbasket.shop.basket.BasketActor.Commands.ByeBye
import com.github.sonenko.shoppingbasket.shop.basket.{Basket, BasketActor}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ShopActor(depot: Depot, createBasketFunc: (ActorRefFactory, Depot) => Basket) extends Actor {
  var baskets: Map[UUID, Basket] = Map()
  val expire = Config.expireBasketsEverySeconds

  context.system.scheduler.schedule(expire seconds, expire seconds)(self ! ShopActor.Commands.ExpireBaskets)

  override def receive: Receive = {
    case c: Command => c match {
      case DropBasket(basketId) =>
        ifBasketExists(basketId) { basket =>
          basket.actor ! BasketActor.Commands.ByeBye(true)
          baskets -= basketId
          sender ! BasketDropSuccess
        }
      case ToBasket(basketId, cmd, forceCreate) =>
        updateBasketUpdatedAt(basketId)
        val fn = if (forceCreate) forceWithBasket _ else ifBasketExists _
        fn(basketId) { basket =>
          basket.actor.tell(cmd, sender)
        }
      case GetState =>
        sender ! ShopState(baskets.keys.toList)
      case ExpireBaskets =>
        val (newBaskets, toEraseBaskets) = baskets.partition(_._2.updatedAt.plusSeconds(expire).isAfter(DateTime.now()))
        baskets = newBaskets
        toEraseBaskets.foreach(x => x._2.actor ! ByeBye(true))
      case Buy(basketId) =>
        ifBasketExists(basketId) { basket =>
          baskets -= basketId
          basket.actor ! ByeBye(false)
          sender ! BuySuccess
        }
    }
  }

  def ifBasketExists(basketId: UUID)(func: Basket => Unit): Unit = {
    baskets.get(basketId) match {
      case Some(basketActor) => func(basketActor)
      case None => sender ! BasketNotFoundError
    }
  }

  def ifBasketNoBasket(basketId: UUID)(func: => Unit): Unit = baskets.get(basketId) match {
    case None => func
    case Some(_) => sender ! BasketAlreadyExistsError
  }

  def forceWithBasket(basketId: UUID)(func: Basket => Unit): Unit = baskets.get(basketId) match {
    case Some(basket) => func(basket)
    case None =>
      val basket = createBasketFunc(context, depot)
      baskets += basketId -> basket
      func(basket)
  }

  def updateBasketUpdatedAt(basketId: UUID): Unit = {
    baskets = baskets.map {
      case (`basketId`, bask) => basketId -> bask.updated
      case x => x
    }
  }
}

object ShopActor {

  def create(ctx: ActorRefFactory, depot: Depot, createBasketFunc: (ActorRefFactory, Depot) => Basket = createBasketFunc): Shop =
    new Shop {
      override val actor = ctx.actorOf(Props(classOf[ShopActor], depot, createBasketFunc))
    }

  private def createBasketFunc(ctx: ActorRefFactory, depot: Depot): Basket =
    BasketActor.create(ctx, depot)

  sealed trait Command

  object Commands {
    case class DropBasket(basketId: UUID) extends Command
    case class ToBasket(basketId: UUID, basketActor: BasketActor.Command, forceCreate: Boolean) extends Command
    case object GetState extends Command
    case object ExpireBaskets extends Command
    case class Buy(basketId: UUID) extends Command
  }
}

trait Shop {
  val actor: ActorRef
}