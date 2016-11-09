package com.github.sonenko.shoppingbasket.integration

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.http.scaladsl.server.Route
import com.github.sonenko.shoppingbasket.depot.DepotActor
import com.github.sonenko.shoppingbasket.{BasketState, Config, DepotState}

import scala.util.Try

/**
  * integration test for `/api/shoppingbasket`
  */
class ShoppingBasketTest extends Integration {

  val good = DepotActor.initialState.head.copy(count = 1)
  val goodId = good.id

  "GET /api/shoppingbasket" should {
    "redirect with status `PermanentRedirect` and add cookie `user-session`" in new Scope {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.PermanentRedirect
        header[`Set-Cookie`].get.cookie.name shouldEqual "user-session-id"
        Try(UUID.fromString(header[`Set-Cookie`].get.cookie.value)).isSuccess shouldEqual true
      }
    }
    "return BadRequest if request contain cookie but no products" in new Scope {
      val cookeId = fetchCookieId(route)
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "POST /api/shoppingbasket" should {
    "respond with status BadRequest if good with specified id not found" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "${java.util.UUID.randomUUID()}", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with BadRequest if goods count is less then 1" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "${java.util.UUID.randomUUID()}", "count": 0}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.PermanentRedirect // TODO - should be BadRequest
      }
    }
    "respond with BadRequest if depot has not such ammount of goods" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": 100}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status Created and return current state of basket, and update state" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = 2)))
      }
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = 2)))
      }
    }
  }

  "DELETE /api/shoppingbasket" should {
    "respond BadRequest if incorrect productId" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "${java.util.UUID.randomUUID()}", "count": 2}""")
      Delete("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status OK and updated state if goods deleted" in new Scope {
      val cookeId = fetchCookieId(route)
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": 2}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = 2)))
      }

      val bodyDel = jsonEntity(s"""{"goodId": "$goodId", "count": 2}""")
      Delete("/api/shoppingbasket", bodyDel) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "After session expiration goods should be placed back to depot" in new Scope {
    val cookeId = fetchCookieId(route)
    val body = jsonEntity(s"""{"goodId": "$goodId", "count": 2}""")
    Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
      status shouldEqual StatusCodes.Created
      responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = 2)))
    }
    Get("/api/products") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[DepotState] shouldNot equal(DepotState(DepotActor.initialState))
    }
    Thread.sleep(3000)
    Get("/api/products") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      entityAs[DepotState] shouldEqual DepotState(DepotActor.initialState)
    }
  }

  "Post /api/shoppingbasket/buy" should {
    "respond with status BadRequest if basket not created" in new Scope {
      val cookeId = fetchCookieId(route)
      Post("/api/shoppingbasket/buy") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "respond with status NoContent in happy case, and empty basket and not return item back to depot" in new Scope {
      // add product
      val cookeId = fetchCookieId(route)
      val count = 2
      val body = jsonEntity(s"""{"goodId": "$goodId", "count": $count}""")
      Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[BasketState] shouldEqual BasketState(List(good.copy(count = count)))
      }
      // good should not be in depot
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        val initialGood = DepotActor.initialState.head
        entityAs[DepotState].goods.head.id shouldEqual initialGood.id
        entityAs[DepotState].goods.head.count shouldEqual (initialGood.count - count)
      }
      // purchase
      Post("/api/shoppingbasket/buy") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.NoContent
      }
      // current state - basket should be deleted
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
      // good should not be in depot
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        val initialGood = DepotActor.initialState.head
        entityAs[DepotState].goods.head.id shouldEqual initialGood.id
        entityAs[DepotState].goods.head.count shouldEqual (initialGood.count - count)
      }
    }
  }

  def fetchCookieId(route: Route): String = {
    var res: String = ""
    Get("/api/shoppingbasket") ~> route ~> check {
      res = header[`Set-Cookie`].get.cookie.value
    }
    res
  }
}
