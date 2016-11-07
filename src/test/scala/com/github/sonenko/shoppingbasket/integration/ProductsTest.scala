package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.StatusCodes
import com.github.sonenko.shoppingbasket.depot.DepotActor

/**
  * integration test for `/api/products`
  */
class ProductsTest extends Integration {
  "GET /api/products" should {
    "respond with `list of current goods` and status `OK` " in {
      Get("/api/products") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[DepotActor.Answers.State] shouldEqual DepotActor.Answers.State(DepotActor.initialState)
      }
    }
  }
}
