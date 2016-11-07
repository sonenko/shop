package com.github.sonenko.shoppingbasket.shop

import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.github.sonenko.shoppingbasket.depot.Depot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Shop(system: ActorSystem, depot: Depot, implicit val timeout: Timeout) {

  val shopActor = system.actorOf(ShopActor.props(depot))

  def createBasket(basketId: UUID): Future[ShopActor.Answers.BasketCreateSuccess] =
    ask(shopActor, ShopActor.Commands.CreateBasket(basketId)).mapTo[ShopActor.Answers.BasketCreateSuccess]

  def listBaskets: Future[List[UUID]] =
    ask(shopActor, ShopActor.Queries.GetState).mapTo[ShopActor.Answers.State].map(x => x.basketIds)
}
