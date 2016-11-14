package com.github.sonenko.shoppingbasket.integration

import java.util.UUID

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, Cookie, `Set-Cookie`}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import com.github.sonenko.shoppingbasket.rest.JsonProtocol
import com.github.sonenko.shoppingbasket.stock.StockActor
import com.github.sonenko.shoppingbasket.{BasketState, Config, Constructor, StockState}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

/** trait to mix for writing integration tests, it mock nothing, but simply run routes, very close to end2end
  */
trait Integration extends WordSpec with Matchers with ScalatestRouteTest with JsonProtocol with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait Scope {
    val constr = new Constructor()
    val route: Route = Route.seal(constr.route)
  }

  val initialCountOfFirstProductsInStock = StockActor.initialState.head.count
  val product = StockActor.initialState.head.copy(count = 1)
  val productId = product.id

  def jsonEntity(contents: String) = HttpEntity(ContentTypes.`application/json`, contents)

  def fetchCookieId(route: Route): UUID =
    Get("/api/shoppingbasket") ~> route ~> check {
      UUID.fromString(header[`Set-Cookie`].get.cookie.value)
    }


  def createBasketAndAddProduct(route: Route, count: Int = 1): UUID = {
    val cookeId = fetchCookieId(route)
    val body = jsonEntity(s"""{"productId": "$productId", "count": $count}""")
    Post("/api/shoppingbasket", body) ~> addHeader(Cookie(Config.cookieNameForSession, cookeId.toString)) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }
    cookeId
  }

  def firstProductCountInStock(route: Route): Int =
    Get("/api/products") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[StockState].products.head.count
    }

  def firstProductCountInBasket(route: Route, basketId: UUID): Int =
    Get(s"/api/admin/baskets/$basketId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[BasketState].products.headOption.map(_.count).getOrElse(0)
    }

  def isBasketExists(route: Route, basketId: UUID): Boolean =
    Get(s"/api/admin/baskets/$basketId") ~> addCredentials(BasicHttpCredentials("admin", "pwd")) ~> route ~> check {
      status == StatusCodes.OK
    }
}
