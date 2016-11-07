package com.github.sonenko.shoppingbasket.shop.basket

import akka.actor.{Actor, PoisonPill, Props}
import com.github.sonenko.shoppingbasket.depot.Depot

class BasketActor(depot: Depot) extends Actor {
  override def receive: Receive = {
    case c: BasketActor.Command => c match {
      case BasketActor.Commands.ByeBye =>
        self ! PoisonPill
    }
  }
}

object BasketActor {
  def props(depot: Depot) = Props(classOf[BasketActor], depot)

  sealed trait Command
  object Commands {
    case object ByeBye extends Command
  }
}