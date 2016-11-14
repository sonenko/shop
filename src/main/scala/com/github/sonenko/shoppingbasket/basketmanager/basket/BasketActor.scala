package com.github.sonenko.shoppingbasket.basketmanager.basket

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, PoisonPill, Props}
import com.github.sonenko.shoppingbasket._
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor.Commands._
import com.github.sonenko.shoppingbasket.basketmanager.basket.BasketActor._
import com.github.sonenko.shoppingbasket.stock.{Prod, Stock, StockActor}
import org.joda.time.DateTime

/**
  * Basket actor - represents current state of basket, every user that has at least one product has his own basket.
  * basket can:
  * - be closed
  * - add product
  * - remove product
  * - view state
  * It speaks to Stock, and ask if stock has specified product or not. if yes and stock has need count of products
  * basket takes this products from stock.
  * when basket closes - it sends items back to stock, if exchange rejected.
  * @param stock - wrapper for StockActor
  * @param stopSn - function that executes to shut down actor
  */
class BasketActor(stock: Stock, stopSn: ActorRef => Unit) extends Actor with ActorLogging {
  var state = BasketState(Nil)

  override def receive: Receive = {
    case c: Command => c match {
      case ByeBye(putProductBack) =>
        beforeStop(putProductBack)
        stopSn(self)
      case AddProduct(productId, count) =>
        stock.actor ! StockActor.Commands.TakeProduct(productId, count)
        context.become(busy(sender))
      case GetState =>
        sender ! state
      case DropProduct(productId, count) =>
        ifProductExistsInBasket(productId){ productInBasket =>
          val productsToRemoveCount = if (productInBasket.count > count) count else productInBasket.count
          stock.actor ! StockActor.Commands.PutProduct(productId, productsToRemoveCount)
          context.become(busy(sender))
        }
    }
  }

  def busy(sndr: ActorRef): Receive = {
    case ByeBye(putProductsBack) =>
      context.become(busyDying(sndr, putProductsBack))
      sender ! GotMeIWillDieAfterDielsWithStock
    case GetState =>
      sender ! state
    case msg: ProductRemoveFromStockSuccess =>
      on(sndr, msg)
    case msg @ ProductNotFoundInStockError =>
      sndr ! msg
      context.unbecome()
    case msg @ ProductAmountIsLowInStockError =>
      sndr ! msg
      context.unbecome()
    case msg: ProductAddToStockSuccess =>
      on(sndr, msg)
    case _: Command =>
      sender ! Busy
  }

  def busyDying(sndr: ActorRef, putProductsBack: Boolean): Receive = {
    case GetState =>
      sender ! state
    case msg: ProductRemoveFromStockSuccess =>
      on(sndr, msg)
      context.unbecome()
      self ! ByeBye(putProductsBack)
    case msg @ ProductNotFoundInStockError =>
      on(sndr, msg)
      context.unbecome()
      self ! ByeBye(putProductsBack)
    case msg @ ProductAmountIsLowInStockError =>
      on(sndr, msg)
      context.unbecome()
      self ! ByeBye(putProductsBack)
    case msg: ProductAddToStockSuccess =>
      on(sndr, msg)
      context.unbecome()
      self ! ByeBye(putProductsBack)
    case _: Command => sender ! Busy
  }

  def ifProductExistsInBasket(productId: UUID)(fn: Prod => Unit): Unit = state.products.find(_.id == productId) match {
    case None => sender ! ProductNotFoundRemoveFromBasketError
    case Some(productInBasket) => fn(productInBasket)
  }

  def beforeStop(putProductsBack: Boolean) = {
    if (putProductsBack) {
      state.products.foreach(product => {
        stock.actor ! StockActor.Commands.PutProduct(product.id, product.count, false)
      })
    }
  }

  def on(sndr: ActorRef, msg: ProductRemoveFromStockSuccess): Unit = {
    val productFromStock = msg.product
    state.products.find(_.id == productFromStock.id) match {
      case None =>
        state = BasketState(productFromStock :: state.products)
      case Some(oldProduct) =>
        val newProducts = state.products.map(x =>
          if (x.id == productFromStock.id) oldProduct.copy(count = oldProduct.count + productFromStock.count)
          else x
        )
        state = BasketState(newProducts)
    }
    sndr ! AddProductToBasketSuccess(state)
    context.unbecome()
  }

  def on(sndr: ActorRef, msg: ProductAddToStockSuccess): Unit = {
    val removedProduct = msg.product
    val productId = removedProduct.id
    val productToRemove = state.products.find(_.id == productId).head
    if (productToRemove.count == removedProduct.count) {
      state = BasketState(state.products.filter(_.id != productId))
    } else {
      state = BasketState(state.products.map {
        case productInBasket @ Prod(`productId`, _, _, oldCount, _, _) =>
          productInBasket.copy(count = oldCount - removedProduct.count)
        case x => x
      })
    }
    sndr ! RemoveProductFromBasketSuccess(state)
    context.unbecome()
  }

  def on(sndr: ActorRef, msg: ProductNotFoundInStockError.type): Unit = {
    sndr ! msg
  }

  def on(sndr: ActorRef, msg: ProductAmountIsLowInStockError.type): Unit = {
    sndr ! msg
  }
}

object BasketActor {
  def create(ctx: ActorRefFactory, stock: Stock, stopSn: ActorRef => Unit = _ ! PoisonPill) = new Basket {
    override val actor = ctx.actorOf(Props(classOf[BasketActor], stock, stopSn))
  }

  sealed trait Command

  object Commands {
    case class ByeBye(putProductsBack: Boolean) extends Command
    case class AddProduct(productId: UUID, count: Int) extends Command
    case class DropProduct(productId: UUID, count: Int) extends Command
    case object GetState extends Command
  }
}

trait Basket {
  val actor: ActorRef
  val updatedAt: DateTime = DateTime.now()
  def isExpired: Boolean = updatedAt.plusSeconds(Config.expireBasketsEverySeconds).isBefore(DateTime.now)
  def updated = new Basket{
    override val actor = Basket.this.actor
  }
}
