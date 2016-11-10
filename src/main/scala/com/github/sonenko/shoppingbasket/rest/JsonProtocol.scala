package com.github.sonenko.shoppingbasket.rest

import java.util.UUID

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.joda.money.{CurrencyUnit, Money}
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, DefaultJsonFormats, ext}

/** Contains implicits convertions case classes to JSON and conversely
  */
trait JsonProtocol extends DefaultJsonFormats with Json4sSupport {

  object JodaMoneySerializer extends CustomSerializer[Money](format => ( {
    case JString(money) =>
      val Array(ammountStr, currencyStr) = money.split(' ')
      val ammount = ammountStr.toDouble
      val currency = CurrencyUnit.of(currencyStr)
      Money.of(currency, ammount)
  }, {
    case money: Money => JString(s"${money.getAmount} ${money.getCurrencyUnit}")
  }))

  object UUIDKeyJSonSerializer extends CustomKeySerializer[UUID](format => ( {
    case s: String => UUID.fromString(s)
  }, {
    case x: UUID => x.toString
  }))

  implicit val serialization = Serialization
  implicit val formats = DefaultFormats + ext.UUIDSerializer + ext.URLSerializer + JodaMoneySerializer + UUIDKeyJSonSerializer
}

case class AddProduct(productId: UUID, count: Int) {
  require(count >= 1)
}
case class DropProduct(productId: UUID, count: Int) {
  require(count >= 1)
}