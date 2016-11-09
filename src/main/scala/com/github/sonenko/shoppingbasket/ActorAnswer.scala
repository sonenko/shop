package com.github.sonenko.shoppingbasket

import java.util.UUID

import com.github.sonenko.shoppingbasket.depot.Good
import org.joda.money.{CurrencyUnit, Money}

sealed trait ActorAnswer

// Shop
case class BasketCreateSuccess(basketId: UUID) extends ActorAnswer
case class ShopState(basketIds: List[UUID]) extends ActorAnswer
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

// Depot
case class DepotState(goods: List[Good]) extends ActorAnswer
case object GoodNotFoundInDepotError extends ActorAnswer
case object GoodAmountIsLowInDepotError extends ActorAnswer
case class GoodRemoveFromDepotSuccess(good: Good) extends ActorAnswer
case class GoodAddToDepotSuccess(good: Good) extends ActorAnswer