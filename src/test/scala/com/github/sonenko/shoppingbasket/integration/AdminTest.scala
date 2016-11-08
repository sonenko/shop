package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.github.sonenko.shoppingbasket.DepotState

/**
  * integration test for `/api/admin`
  */
class AdminTest extends Integration {

  "GET /api/admin/sessions" should {
    "respond with status `Unauthorized` if no credentials provided" in new Scope {
      Get("/api/admin/sessions") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status `Unauthorized` if no credentials is wrong" in new Scope  {
      Get("/api/admin/sessions") ~> addCredentials(BasicHttpCredentials("admin", "wrongPass")) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status OK and return empty array if good credentials" in new Scope {
      Get("/api/admin/sessions") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[DepotState] shouldEqual DepotState(Nil)
      }
    }
  }
}
