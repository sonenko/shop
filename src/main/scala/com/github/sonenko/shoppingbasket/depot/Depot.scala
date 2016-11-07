package com.github.sonenko.shoppingbasket.depot

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

class Depot(system: ActorSystem, implicit val timeout: Timeout) {

  val depotActor = system.actorOf(DepotActor.props)

  def getState: Future[DepotActor.Answers.State] =
    ask(depotActor, DepotActor.Queries.GetState).mapTo[DepotActor.Answers.State]
}
