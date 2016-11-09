package com.github.sonenko.shoppingbasket
package rest

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.github.sonenko.shoppingbasket.basketmanager.BasketManager
import com.github.sonenko.shoppingbasket.stock.Stock
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * main route that combine other routes for rest
  *
  * @param log   - logger
  * @param stock - wrapper for StockActor
  * @param basketManager  - wrapper for BasketManagerActor
  */
class RootRoute(val log: LoggingAdapter, val stock: Stock, val basketManager: BasketManager) extends JsonProtocol
  with ShoppingBasketRoute with ProductsRoute with AdminRoute {

  def route = shoppingBasketRoute ~ productsRoute ~ adminRoute

  def inquire(who: ActorRef, msg: Any, pf: PartialFunction[Any, HttpResponse] = PartialFunction.empty): Future[HttpResponse] =
    inquireInternal(who, msg).map(x => actorAnswerToRest(x , pf))

  protected implicit def triple2Response(t: (StatusCode, ActorAnswer, List[HttpHeader])): HttpResponse = HttpResponse(
    status = t._1,
    entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(t._2)),
    headers = t._3
  )

  protected implicit def tupleActorAnswer2Response(t: (StatusCode, ActorAnswer)): HttpResponse = HttpResponse(
    status = t._1,
    entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(t._2))
  )

  protected implicit def tupleString2Response(t: (StatusCode, String)): HttpResponse =
    HttpResponse(status = t._1, entity = t._2)

  protected implicit def actorAnswer2Response(a: ActorAnswer): HttpResponse =
    HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(a)))

  protected implicit def statusCodeToResponce(c: StatusCode): HttpResponse = HttpResponse(status = c)

  protected def inquireInternal(who: ActorRef, msg: Any): Future[ActorAnswer] =
    ask(who, msg).mapTo[ActorAnswer]

  private def actorAnswerToRest(actorAnswer: ActorAnswer, pf: PartialFunction[Any, HttpResponse]): HttpResponse =
    (pf orElse ({
      case msg: BasketManagerState => msg
      case AddGoodToBasketSuccess(state) => StatusCodes.Created -> state
      case msg: BasketState => msg
      case Busy => StatusCodes.TooManyRequests -> "previous request in progress, be patient"
      case msg: StockState => msg
      case GoodNotFoundInStockError | GoodAmountIsLowInStockError => StatusCodes.BadRequest
      case BasketNotFoundError => StatusCodes.BadRequest
      case RemoveGoodFromBasketSuccess(state) => state
      case BasketDropSuccess => StatusCodes.NoContent
      case x: ActorAnswer =>
        val errorMsg = s"unexpected case class received to rest $x"
        log.warning(errorMsg)
        StatusCodes.InternalServerError -> errorMsg
    }:PartialFunction[Any, HttpResponse])).apply(actorAnswer)
}
