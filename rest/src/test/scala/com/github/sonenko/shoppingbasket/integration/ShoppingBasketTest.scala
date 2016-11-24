package com.github.sonenko.shoppingbasket.integration

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{BasketState, Config}

import scala.util.Try

/**
  * integration test for `/api/shoppingbasket`
  */
class ShoppingBasketTest extends Integration {

  "GET /api/shoppingbasket" should {
    "redirect with status `PermanentRedirect` and set cookie `user-session`" in new Scope {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.PermanentRedirect
        header[`Set-Cookie`].get.cookie.name shouldEqual "user-session-id"
        Try(UUID.fromString(header[`Set-Cookie`].get.cookie.value)).isSuccess shouldEqual true
      }
    }
    "respond with Status OK, and return empty basket state" in new Scope {
      val cookeId = fetchCookieId(route)
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(Nil)
      }
    }
    "respond with status OK, and return not empty basket state" in new Scope {
      val cookieId = createBasketAndAddProduct(route)
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(List(product))
      }
    }
  }

  "POST /api/shoppingbasket" should {
    "respond with status BadRequest if product with specified id not found" in new Scope {
      val cookieId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${UUID.randomUUID()}", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if products count is less then 1" in new Scope {
      val cookieId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${UUID.randomUUID()}", "count": 0}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if stock has not such ammount of products" in new Scope {
      val cookieId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "$productId", "count": 100}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status Created and return current state of basket, and update state" in new Scope {
      val cookieId = fetchCookieId(route)
      val countInBasket = 2
      val body = jsonEntity(s"""{"productId": "$productId", "count": $countInBasket}""")
      // Created + current state
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = countInBasket)))
      }
      // if state updated
      firstProductCountInBasket(route, cookieId) shouldEqual countInBasket
      // item removed from stock
      firstProductCountInStock(route) shouldEqual (StockActor.initialState.head.count - countInBasket)
    }
  }

  "DELETE /api/shoppingbasket" should {
    "respond BadRequest if incorrect productId" in new Scope {
      val cookieId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${UUID.randomUUID()}", "count": 2}""")
      Delete("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if count to remove is less then 1" in new Scope {
      val cookieId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "$productId", "count": 0}""")
      Delete("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status OK + updated State, put items back to depot" in new Scope {
      val itemsAddedToBasket = 3
      val itemsRemovedFromBasket = 2
      val expectedItemsInBasket = itemsAddedToBasket - itemsRemovedFromBasket
      val cookieId = createBasketAndAddProduct(route, 3) // create + add 3 products
      val bodyDel = jsonEntity(s"""{"productId": "$productId", "count": $itemsRemovedFromBasket}""")
      Delete("/api/shoppingbasket", bodyDel) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState].products.head.count shouldEqual expectedItemsInBasket
      }
      // check state one more time
      firstProductCountInBasket(route, cookieId) shouldEqual expectedItemsInBasket
      firstProductCountInStock(route) shouldEqual (initialCountOfFirstProductsInStock - expectedItemsInBasket)
    }
    "remove all items if item count bigger then in basket" in new Scope {
      val cookieId = createBasketAndAddProduct(route, 2) // create + add 2 products
      val bodyDel = jsonEntity(s"""{"productId": "$productId", "count": 10}""")
      Delete("/api/shoppingbasket", bodyDel) ~> addHeader(Cookie(Config.cookieNameForSession, cookieId.toString)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState].products shouldEqual Nil
      }
      // check state one more time
      firstProductCountInBasket(route, cookieId) shouldEqual 0
      firstProductCountInStock(route) shouldEqual initialCountOfFirstProductsInStock
    }
  }

  "After session expiration products should be placed back to stock" in new Scope {
    val cookieId = createBasketAndAddProduct(route, 2) // create + add 2 products
    // check if products in basket and not in stock
    firstProductCountInBasket(route, cookieId) shouldEqual 2
    firstProductCountInStock(route) shouldEqual (initialCountOfFirstProductsInStock - 2)
    Thread.sleep(2000)
    // now our basket should be empty(not exists) and products should be in stock
    isBasketExists(route, cookieId) shouldEqual false
    firstProductCountInStock(route) shouldEqual initialCountOfFirstProductsInStock
  }
}
