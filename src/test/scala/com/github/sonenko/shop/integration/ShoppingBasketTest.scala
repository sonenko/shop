package com.github.sonenko.shop.integration

import akka.http.scaladsl.model.StatusCodes

/**
  * integration test for `/api/shoppingbasket`
  */
class ShoppingBasketTest extends Integration {
  "GET /api/shoppingbasket" should {
    "respond with `[]`:200 " in {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual "[]"
      }
    }
  }
}
