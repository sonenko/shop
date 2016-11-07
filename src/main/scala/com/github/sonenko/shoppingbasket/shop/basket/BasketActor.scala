package com.github.sonenko.shoppingbasket.shop.basket

import akka.actor.{Actor, Props}
import com.github.sonenko.shoppingbasket.depot.Depot

class BasketActor(depot: Depot) extends Actor {
  override def receive: Receive = {
    case "hello" =>
  }
}

object BasketActor {
  def props(depot: Depot) = Props(classOf[BasketActor], depot)
}