package com.github.sonenko.shoppingbasket

import com.typesafe.config.ConfigFactory

object Config {
  val cfg = ConfigFactory.load()
  val host = cfg.getString("shoppingbasket.host")
  val port = cfg.getInt("shoppingbasket.port")
  val cookieNameForSession = cfg.getString("shoppingbasket.cookieNameForSession")
  val adminName = cfg.getString("shoppingbasket.admin.name")
  val adminPassword = cfg.getString("shoppingbasket.admin.password")
  val timeout = cfg.getInt("shoppingbasket.timeoutInSec")
}
