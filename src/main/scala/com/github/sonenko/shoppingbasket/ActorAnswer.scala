package com.github.sonenko.shoppingbasket

import java.util.UUID

import com.github.sonenko.shoppingbasket.stock.Good
import org.joda.money.{CurrencyUnit, Money}

sealed trait ActorAnswer

// Basketmanager
case class BasketCreateSuccess(basketId: UUID) extends ActorAnswer
case class BasketManagerState(basketIds: List[UUID]) extends ActorAnswer
case object BasketDropSuccess extends ActorAnswer
case object BasketAlreadyExistsError extends ActorAnswer
case object BasketNotFoundError extends ActorAnswer
case object BuySuccess extends ActorAnswer

// Basket
case class AddGoodToBasketSuccess(state: BasketState) extends ActorAnswer
case class RemoveGoodFromBasketSuccess(state: BasketState) extends ActorAnswer
case object RemoveGoodFromBasketErrorNotFountGood extends ActorAnswer
case class BasketState(goods: List[Good], total: Money) extends ActorAnswer
object BasketState {
  def apply(goods: List[Good]): BasketState = BasketState(
    goods = goods,
    goods.foldLeft(Money.ofMinor(CurrencyUnit.USD, 0L))((acc: Money, good: Good) =>
      acc.plus(good.price.multipliedBy(good.count)))
  )
}
case object Busy extends ActorAnswer

// Stock
case class StockState(goods: List[Good]) extends ActorAnswer
case object GoodNotFoundInStockError extends ActorAnswer
case object GoodAmountIsLowInStockError extends ActorAnswer
case class GoodRemoveFromStockSuccess(good: Good) extends ActorAnswer
case class GoodAddToStockSuccess(good: Good) extends ActorAnswer