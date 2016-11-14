package com.github.sonenko.shoppingbasket.integration

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{BasketManagerState, BasketState}

/**
  * integration test for `/api/admin`
  */
class AdminTest extends Integration {

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
    "respond with status OK and return empty BasketManagerState" in new Scope {
      Get("/api/admin/baskets") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketManagerState] shouldEqual BasketManagerState(Nil)
        // same as previously, but better because responseAs[StockState] shouldEqual StockState(Nil) - works
        responseAs[Map[String, Any]] shouldEqual Map("basketIds" -> Nil)
      }
    }
    "respond with status OK and return filled BasketManagerState" in new Scope {
      val basketId = createBasketAndAddProduct(route)
      Get("/api/admin/baskets") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketManagerState] shouldEqual BasketManagerState(List(basketId))
      }
    }
  }

  "GET /api/admin/baskets/{basketId}" should {
    "respond with status `Unauthorized` if bad credentials provided" in new Scope {
      Get(s"/api/admin/baskets/${UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "wrongPass")) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "return NotFound if there is no such session with products" in new Scope {
      Get(s"/api/admin/baskets/${java.util.UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "return State of basket in happy case" in new Scope {
      val basketId = createBasketAndAddProduct(route)
      Get(s"/api/admin/baskets/$basketId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[BasketState] shouldEqual BasketState(List(product.copy(count = 1)))
      }
    }
  }

  "DELETE /api/admin/baskets/{basketId}" should {
    "respond with status `Unauthorized` if bad credentials provided" in new Scope {
      Get(s"/api/admin/baskets/${UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "wrongPass")) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "return NotFound if basket not found" in new Scope {
      Delete(s"/api/admin/baskets/${UUID.randomUUID}") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "respond with NoContent. Remove basket. Not put products back to Stock" in new Scope {
      val initialCountOfProductInStock = StockActor.initialState.head.count

      // should take 1 product from stock
      val basketId = createBasketAndAddProduct(route)
      firstProductCountInStock(route) shouldEqual (initialCountOfProductInStock - 1)

      // should respond with NoContent + remove basket
      Delete(s"/api/admin/baskets/$basketId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.NoContent
      }
      isBasketExists(route, basketId) shouldEqual false

      // should not put products back to stock
      firstProductCountInStock(route) shouldEqual (initialCountOfProductInStock - 1)
    }
  }
}
