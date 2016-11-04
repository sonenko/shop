package com.github.sonenko.shop.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.sonenko.shop.Constructor
import org.scalatest.{Matchers, WordSpec}

class ShopIntegrationTest extends WordSpec with Matchers with ScalatestRouteTest {
  val route = Route.seal(new Constructor().rootRoute)

  "GET /api/shoppingbasket" should {
    "respond with `[]`:200 " in {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual "[]"
      }
    }
  }
}
