package com.github.sonenko.shoppingbasket
package basketmanager

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor.Commands.{ExpireBaskets, _}
import com.github.sonenko.shoppingbasket.basketmanager.BasketManagerActor._
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor.Commands.ByeBye
import com.github.sonenko.shoppingbasket.basketmanager.basket.{Basket, BasketActor}
import com.github.sonenko.shoppingbasket.stock.Stock
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class BasketManagerActor(stock: Stock, createBasketFunc: (ActorRefFactory, Stock) => Basket) extends Actor {
  var baskets: Map[UUID, Basket] = Map()
  val expire = Config.expireBasketsEverySeconds

  context.system.scheduler.schedule(expire seconds, expire seconds)(self ! BasketManagerActor.Commands.ExpireBaskets)

  override def receive: Receive = {
    case c: Command => c match {
      case DropBasket(basketId, putToStock) =>
        ifBasketExists(basketId) { basket =>
          basket.actor ! ByeBye(putToStock)
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
        sender ! BasketManagerState(baskets.keys.toList)
      case ExpireBaskets =>
        val (newBaskets, toEraseBaskets) = baskets.partition(_._2.updatedAt.plusSeconds(expire).isAfter(DateTime.now()))
        baskets = newBaskets
        toEraseBaskets.foreach(x => x._2.actor ! ByeBye(true))
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
      val basket = createBasketFunc(context, stock)
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

object BasketManagerActor {

  def create(ctx: ActorRefFactory, stock: Stock, createBasketFunc: (ActorRefFactory, Stock) => Basket = createBasketFunc): BasketManager =
    new BasketManager {
      override val actor = ctx.actorOf(Props(classOf[BasketManagerActor], stock, createBasketFunc))
    }

  private def createBasketFunc(ctx: ActorRefFactory, stock: Stock): Basket =
    BasketActor.create(ctx, stock)

  sealed trait Command

  object Commands {
    case class DropBasket(basketId: UUID, putItemsToStock: Boolean = true) extends Command
    case class ToBasket(basketId: UUID, basketActor: BasketActor.Command, forceCreate: Boolean) extends Command
    case object GetState extends Command
    case object ExpireBaskets extends Command
  }
}

trait BasketManager {
  val actor: ActorRef
}