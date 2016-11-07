package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials

/**
  * integration test for `/api/admin`
  */
class AdminTest extends Integration {

  "GET /api/admin/sessions" should {
    "respond with status `Unauthorized` if no credentials provided" in {
      Get("/api/admin/sessions") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status `Unauthorized` if no credentials is wrong" in {
      Get("/api/admin/sessions") ~> addCredentials(BasicHttpCredentials("admin", "wrongPass")) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
    "respond with status OK and return empty array if good credentials" in {
      Get("/api/admin/sessions") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[List[String]] shouldEqual Nil
      }
    }
  }
}
