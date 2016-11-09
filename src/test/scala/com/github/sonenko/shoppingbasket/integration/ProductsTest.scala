package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.StatusCodes
import com.github.sonenko.shoppingbasket.StockState
import com.github.sonenko.shoppingbasket.stock.StockActor

/**
  * integration test for `/api/products`
  */
class ProductsTest extends Integration {
  "GET /api/products" should {
    "respond with `list of current goods` and status `OK` " in new Scope {
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[StockState] shouldEqual StockState(StockActor.initialState)
      }
    }
  }
}
