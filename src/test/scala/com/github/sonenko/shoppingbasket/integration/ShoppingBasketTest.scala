package com.github.sonenko.shoppingbasket.integration

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{BasketState, Config, StockState}

import scala.util.Try

/**
  * integration test for `/api/shoppingbasket`
  */
class ShoppingBasketTest extends Integration {

  val product = StockActor.initialState.head.copy(count = 1)
  val productId = product.id

  "GET /api/shoppingbasket" should {
    "redirect with status `PermanentRedirect` and add cookie `user-session`" in new Scope {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.PermanentRedirect
        header[`Set-Cookie`].get.cookie.name shouldEqual "user-session-id"
        Try(UUID.fromString(header[`Set-Cookie`].get.cookie.value)).isSuccess shouldEqual true
      }
    }
    "return empty array if request contain cookie but no products" in new Scope {
      val cookeId = fetchCookieId(route)
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(Nil)
      }
    }
  }

  "POST /api/shoppingbasket" should {
    "respond with status BadRequest if product with specified id not found" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${java.util.UUID.randomUUID()}", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if products count is less then 1" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${java.util.UUID.randomUUID()}", "count": 0}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if stock has not such ammount of products" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "$productId", "count": 100}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status Created and return current state of basket, and update state" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "$productId", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = 2)))
      }
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = 2)))
      }
    }
  }

  "DELETE /api/shoppingbasket" should {
    "respond BadRequest if incorrect productId" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "${java.util.UUID.randomUUID()}", "count": 2}""")
      Delete("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status OK and updated state if products deleted" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"productId": "$productId", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = 2)))
      }

      val bodyDel = jsonEntity(s"""{"productId": "$productId", "count": 2}""")
      Delete("/api/shoppingbasket", bodyDel) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "tup back correct number of products" in new Scope {
      val toAddCount = 3
      val toRemoveCount = 1

      val cookeId = fetchCookieId(route)
      // add
      val body = jsonEntity(s"""{"productId": "$productId", "count":$toAddCount}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = toAddCount)))
      }

      val bodyDel = jsonEntity(s"""{"productId": "$productId", "count": $toRemoveCount}""")
      Delete("/api/shoppingbasket", bodyDel) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
      // check count in basket
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState].products.head.count shouldEqual (toAddCount - toRemoveCount)
      }
      // check count in Stock
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        val initialProduct = StockActor.initialState.head
        entityAs[StockState].products.head.id shouldEqual initialProduct.id
        entityAs[StockState].products.head.count shouldEqual (initialProduct.count - (toAddCount - toRemoveCount))
      }
    }
  }

  "After session expiration products should be placed back to stock" in new Scope {
    val cookeId = fetchCookieId(route)
    val body = jsonEntity(s"""{"productId": "$productId", "count": 2}""")
    Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
      status shouldEqual StatusCodes.Created
      responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = 2)))
    }
    Get("/api/products") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[StockState] shouldNot equal(StockState(StockActor.initialState))
    }
    Thread.sleep(1500)
    Get("/api/products") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[StockState] shouldEqual StockState(StockActor.initialState)
    }
  }
}
