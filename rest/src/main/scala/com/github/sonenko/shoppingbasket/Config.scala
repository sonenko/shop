package com.github.sonenko.shoppingbasket

import com.typesafe.config.ConfigFactory

object Config {
  private val cfg = ConfigFactory.load()
  val host: String = cfg.getString("shoppingbasket.host")
  val port: Int = cfg.getInt("shoppingbasket.port")
  val cookieNameForSession: String = cfg.getString("shoppingbasket.cookieNameForSession")
  val adminName: String = cfg.getString("shoppingbasket.admin.name")
  val adminPassword: String = cfg.getString("shoppingbasket.admin.password")
  val timeout: Int = cfg.getInt("shoppingbasket.timeoutInSec")
  val expireBasketsEverySeconds: Int = cfg.getInt("shoppingbasket.expireBasketsEverySeconds")
}
