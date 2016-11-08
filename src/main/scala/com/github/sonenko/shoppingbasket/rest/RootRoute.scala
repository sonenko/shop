package com.github.sonenko.shoppingbasket
package rest

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.github.sonenko.shoppingbasket.depot.Depot
import com.github.sonenko.shoppingbasket.shop.Shop
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * main route that combine other routes for rest
  *
  * @param log   - logger
  * @param depot - wrapper for DepotActor
  * @param shop  - wrapper for ShopActor
  */
class RootRoute(val log: LoggingAdapter, val depot: Depot, val shop: Shop) extends JsonProtocol
  with ShoppingBasketRoute with ProductsRoute with AdminRoute {

  def route = shoppingBasketRoute ~ productsRoute ~ adminRoute

  def inquire(who: ActorRef, msg: Any): Future[HttpResponse] = inquireInternal(who, msg) map actorAnswerToRest

  private implicit def triple2Response(t: (StatusCode, ActorAnswer, List[HttpHeader])): HttpResponse = HttpResponse(
    status = t._1,
    entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(t._2)),
    headers = t._3
  )

  private implicit def tupleActorAnswer2Response(t: (StatusCode, ActorAnswer)): HttpResponse = HttpResponse(
    status = t._1,
    entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(t._2))
  )

  private implicit def tupleString2Response(t: (StatusCode, String)): HttpResponse =
    HttpResponse(status = t._1, entity = t._2)

  private implicit def actorAnswer2Response(a: ActorAnswer): HttpResponse =
    HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(a)))

  private implicit def statusCodeToResponce(c: StatusCode): HttpResponse = HttpResponse(status = c)

  private def inquireInternal(who: ActorRef, msg: Any): Future[ActorAnswer] =
    ask(who, msg).mapTo[ActorAnswer]

  private def actorAnswerToRest(actorAnswer: ActorAnswer): HttpResponse = actorAnswer match {
    case msg: ShopState => msg
    case AddGoodToBasketSuccess(state) => StatusCodes.Created -> state
    case msg: BasketState => msg
    case Busy => StatusCodes.TooManyRequests -> "previous request in progress, be patient"
    case msg: DepotState => msg
    case GoodNotFoundInDepotError | GoodAmountIsLowInDepotError => StatusCodes.BadRequest
    case BasketNotFoundError => BasketState(Nil)
    case x: ActorAnswer =>
      val errorMsg = s"unexpected case class received to rest $x"
      log.warning(errorMsg)
      StatusCodes.InternalServerError -> errorMsg
  }
}
