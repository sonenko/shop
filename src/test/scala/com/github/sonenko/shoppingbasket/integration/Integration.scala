package com.github.sonenko.shoppingbasket.integration

import akka.http.scaladsl.model.headers.`Set-Cookie`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.sonenko.shoppingbasket.Constructor
import com.github.sonenko.shoppingbasket.rest.JsonProtocol
import org.scalatest.{Matchers, WordSpec}

/** trait to mix for writing integration tests, it mock nothing, but simply run routes, very close to end2end
  */
trait Integration extends WordSpec with Matchers with ScalatestRouteTest with JsonProtocol {

  trait Scope {
    val route: Route = Route.seal(new Constructor().route)
  }

  def jsonEntity(contents: String) = HttpEntity(ContentTypes.`application/json`, contents)

  def fetchCookieId(route: Route): String = {
    var res: String = ""
    Get("/api/shoppingbasket") ~> route ~> check {
      res = header[`Set-Cookie`].get.cookie.value
    }
    res
  }
}
