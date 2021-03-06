package com.github.sonenko.shoppingbasket
package stock

import java.net.URL
import java.util.UUID
import java.util.UUID.fromString

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import com.github.sonenko.shoppingbasket.stock.StockActor.Commands._
import com.github.sonenko.shoppingbasket.stock.StockActor._
import org.joda.money.{CurrencyUnit, Money}

/** Actor that stores stock state.
  * using this actor we can concurrently:
  *  - take products from stock
  *  - view stock state
  *  - put products back (if exchange rejected)
  */
class StockActor extends Actor with ActorLogging {
  var state: List[Prod] = initialState

  val receive: Receive = {
    case c: Command => c match {
      case GetState =>
        sender ! StockState(state)
      case TakeProduct(productId, count) =>
        ifProductExists(productId) { product =>
          ifProductCountEnough(product, count){
            state = state.map {
              case product: Prod if product.id == productId => product.copy(count = product.count - count)
              case x => x
            }
            sender ! ProductRemoveFromStockSuccess(product.copy(count = count))
          }
        }
      case m@ PutProduct(productId, count, doReplay) =>
        ifProductExists(productId) { product =>
          state = state.map{
            case productInStock @ Prod(`productId`, _, _, oldCount, _, _) =>
              productInStock.copy(count = oldCount + count)
            case x => x
          }
          if (doReplay) sender ! ProductAddToStockSuccess(product.copy(count = count))
        }
    }
  }

  def ifProductExists(productId: UUID)(fn: Prod => Unit): Unit = {
    state.find(_.id == productId) match {
      case None =>  sender ! ProductNotFoundInStockError
      case Some(x) => fn(x)
    }
  }

  def ifProductCountEnough(product: Prod, count: Int)(fn: => Unit): Unit = {
    if (product.count >= count) fn
    else sender ! ProductAmountIsLowInStockError
  }
}

object StockActor {

  def create(system: ActorRefFactory) = new Stock {
    override val actor: ActorRef = system.actorOf(Props(classOf[StockActor]))
  }

  val initialState: List[Prod] = {
    def usd(amount: Long) = Money.ofMajor(CurrencyUnit.USD, amount)
    List(
      Prod(fromString("b41623a7-a5f4-4fb3-8e0e-b18f94b9f184"), "2016 Audi A3", usd(36000), 5, new URL("https://www.cstatic-images.com/car-pictures/main/USC50AUC152A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("e8c73ee0-b4d7-4d18-82de-e6694f94463f"), "2016 Mercedes-Benz E-Class", usd(62250), 10, new URL("https://www.cstatic-images.com/car-pictures/main/USC40MBC682A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("8b098197-93a5-4663-8a09-88209ed2a3d2"), "2016 Porsche Boxster", usd(52100), 1, new URL("https://www.cstatic-images.com/car-pictures/main/USC30PRC021A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("db207ba9-5fe4-40ad-a580-a4d14001dd92"), "2015 Aston Martin DB9", usd(203295), 5, new URL("https://www.cstatic-images.com/car-pictures/main/USC40ANC071A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("04c3e461-d58a-4616-87cc-9c1061f2bd61"), "2016 Audi TT", usd(46400), 3, new URL("https://www.cstatic-images.com/car-pictures/main/USC60AUC112A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("d42f1301-e9a4-4bf3-a9be-7cdc518829cc"), "2016 BMW 228", usd(38650), 15, new URL("https://www.cstatic-images.com/car-pictures/main/USC40BMC621A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("9b9f76e5-ae60-4b2a-b74f-0d55859d4414"), "2015 BMW 428", usd(48750), 20, new URL("https://www.cstatic-images.com/car-pictures/main/USC40BMC582A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("75684634-20cc-4dad-9f83-0e3c2a6ebe1d"), "2016 BMW 650", usd(96200), 7, new URL("https://www.cstatic-images.com/car-pictures/main/USC60BMC281A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("8d286895-3dcb-4d36-9c48-7afa8d2ef2bf"), "2016 Buick Cascada", usd(33065), 2, new URL("https://www.cstatic-images.com/car-pictures/main/USC60BUC181B021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
      Prod(fromString("3d450324-6fd5-48b1-9866-6ad312ea2af3"), "2016 Chevrolet Corvette", usd(59400), 7, new URL("https://www.cstatic-images.com/car-pictures/main/USC40CHC371A021001.png"), "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    )
  }

  trait Command

  object Commands {
    case class TakeProduct(productId: UUID, count: Int) extends Command
    case class PutProduct(productId: UUID, count: Int, doReplay: Boolean = true) extends Command
    case object GetState extends Command
  }
}

trait Stock {
  val actor: ActorRef
}

case class Prod(id: UUID, name: String, price: Money, count: Int, image: URL, description: String)