package com.github.sonenko

import akka.util.Timeout

import scala.concurrent.duration._

package object shoppingbasket {
  implicit val timeout = Timeout(Config.timeout seconds)
}
