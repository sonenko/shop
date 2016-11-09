package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, Cookie}
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{BasketState, Config, StockState}

/**
  * integration test for `/api/admin`
  */
class AdminTest extends Integration {

  val good = StockActor.initialState.head.copy(count = 1)
  val goodId = good.id

  "GET /api/admin/baskets" should {
    "respond with status `Unauthorized` if no credentials provided" in new Scope {
      Get("/api/admin/baskets") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status `Unauthorized` if no credentials is wrong" in new Scope {
      Get("/api/admin/baskets") ~> addCredentials(BasicHttpCredentials("admin", "wrongPass")) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status OK and return empty array if good credentials" in new Scope {
      Get("/api/admin/baskets") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[StockState] shouldEqual StockState(Nil)
      }
    }
  }

  "GET /api/admin/baskets/{basketId}" should {
    "return NotFount if there is no such session with products" in new Scope {
      Get(s"/api/admin/baskets/${java.util.UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "return State of basket in happy case" in new Scope {
      // create basket
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
      }
      // check it
      Get(s"/api/admin/baskets/$cookeId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = 2)))
      }
    }
  }

  "DELETE /api/admin/baskets/{basketId}" should {
    "return NotFound if basket not found" in new Scope {
      Delete(s"/api/admin/baskets/${java.util.UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "respond with no content and not not put products back to Stock(meaning exchange done)" in new Scope {
      val count = 2
      // create basket
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": $count}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
      }
      // good should not be in stock
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        val initialGood = StockActor.initialState.head
        entityAs[StockState].goods.head.id shouldEqual initialGood.id
        entityAs[StockState].goods.head.count shouldEqual (initialGood.count - count)
      }

      // meaning exchange
      Delete(s"/api/admin/baskets/$cookeId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NoContent
      }

      // current state - basket should be deleted
      Get(s"/api/admin/baskets/$cookeId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      // good should not be in stock
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        val initialGood = StockActor.initialState.head
        entityAs[StockState].goods.head.id shouldEqual initialGood.id
        entityAs[StockState].goods.head.count shouldEqual (initialGood.count - count)
      }
    }
  }
}
