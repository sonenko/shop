package com.github.sonenko.shop.integration

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.sonenko.shop.Constructor
import com.github.sonenko.shop.rest.JsonProtocol
import org.scalatest.{Matchers, WordSpec}

/** trait to mix for writing integration tests, it mock nothing, but simply run routes, very close to end2end
  */
trait Integration extends WordSpec with Matchers with ScalatestRouteTest with JsonProtocol {
  val route = Route.seal(new Constructor().route)
}
