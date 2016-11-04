package com.github.sonenko.shop

import com.typesafe.config.ConfigFactory

object Config {
  val cfg = ConfigFactory.load()
  val host = cfg.getString("shop.host")
  val port = cfg.getInt("shop.port")
  val cookieNameForSession = cfg.getString("shop.cookieNameForSession")
}
