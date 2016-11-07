package com.github.sonenko.shoppingbasket.shop

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.ShopActor.Answers.BasketCreateSuccess

import scala.concurrent.Future

class Shop(system: ActorSystem, depot: Depot, implicit val timeout: Timeout) {
  val shopActor = system.actorOf(ShopActor.props(depot))

  def createBasket(): Future[BasketCreateSuccess] =
    ask(shopActor, ShopActor.Commands.CreateBasket).mapTo[BasketCreateSuccess]

}
