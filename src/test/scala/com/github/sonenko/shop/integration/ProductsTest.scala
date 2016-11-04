package com.github.sonenko.shop.integration

import akka.http.scaladsl.model.StatusCodes
import com.github.sonenko.shop.depot.Depot

/**
  * integration test for `/api/products`
  */
class ProductsTest extends Integration {
  "GET /api/products" should {
    "respond with `list of current goods` and status `OK` " in {
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Depot.Answers.State] shouldEqual Depot.Answers.State(Depot.initialState)
      }
    }
  }
}
