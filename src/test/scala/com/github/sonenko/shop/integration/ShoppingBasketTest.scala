package com.github.sonenko.shop.integration

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import com.github.sonenko.shop.Config

import scala.util.Try
/**
  * integration test for `/api/shoppingbasket`
  */
class ShoppingBasketTest extends Integration {

  "GET /api/shoppingbasket" should {
    "redirect with status `PermanentRedirect` and add cookie `user-session`" in {
      Get("/api/shoppingbasket") ~> route ~> check {
        status shouldEqual StatusCodes.PermanentRedirect
        header[`Set-Cookie`].get.cookie.name shouldEqual "user-session-id"
        Try(UUID.fromString(header[`Set-Cookie`].get.cookie.value)).isSuccess shouldEqual true
      }
    }
    "return list of products in basket if request contain cookie" in {
      val cookeId = fetchCookieId()
      Get("/api/shoppingbasket") ~> addHeader(Cookie(Config.cookieNameForSession, cookeId)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "[]"
      }
    }
  }

  def fetchCookieId(): String = {
    var res: String = ""
    Get("/api/shoppingbasket") ~> route ~> check {
      res = header[`Set-Cookie`].get.cookie.value
    }
    res
  }
}
