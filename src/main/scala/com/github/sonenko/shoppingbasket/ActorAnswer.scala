package com.github.sonenko.shoppingbasket

import java.util.UUID

import com.github.sonenko.shoppingbasket.stock.Prod
import org.joda.money.{CurrencyUnit, Money}

sealed trait ActorAnswer

// Basketmanager
case class BasketCreateSuccess(basketId: UUID) extends ActorAnswer
case class BasketManagerState(basketIds: List[UUID]) extends ActorAnswer
case object BasketDropSuccess extends ActorAnswer
case object BasketAlreadyExistsError extends ActorAnswer
case object BasketNotFoundError extends ActorAnswer

// Basket
case class AddProductToBasketSuccess(state: BasketState) extends ActorAnswer
case class RemoveProductFromBasketSuccess(state: BasketState) extends ActorAnswer
case object ProductNotFoundRemoveFromBasketError extends ActorAnswer
case class BasketState(products: List[Prod], total: Money) extends ActorAnswer
object BasketState {
  def apply(products: List[Prod]): BasketState = BasketState(
    products = products,
    products.foldLeft(Money.ofMinor(CurrencyUnit.USD, 0L))((acc: Money, product: Prod) =>
      acc.plus(product.price.multipliedBy(product.count)))
  )
}
case object Busy extends ActorAnswer

// Stock
case class StockState(products: List[Prod]) extends ActorAnswer
case object ProductNotFoundInStockError extends ActorAnswer
case object ProductAmountIsLowInStockError extends ActorAnswer
case class ProductRemoveFromStockSuccess(product: Prod) extends ActorAnswer
case class ProductAddToStockSuccess(product: Prod) extends ActorAnswer