package com.github.sonenko.shoppingbasket.depot

import java.net.URL
import java.util.UUID
import java.util.UUID.fromString

import akka.actor.{Actor, Props}
import com.github.sonenko.shoppingbasket.depot.DepotActor._
import org.joda.money.{CurrencyUnit, Money}

/** serves as Depot
  */
class DepotActor extends Actor {
  var state: List[Good] = DepotActor.initialState

  val receive: Receive = {
    case q: Query => q match {
      case Queries.GetState => sender ! Answers.State(state)
    }
  }
}

object DepotActor {

  val initialState = {
    def usd(amount: Long) = Money.ofMajor(CurrencyUnit.USD, amount)
    List(
      Good(fromString("b41623a7-a5f4-4fb3-8e0e-b18f94b9f184"), "2016 Audi A3", usd(36000), 5, new URL("https://www.cstatic-images.com/car-pictures/main/USC50AUC152A021001.png")),
      Good(fromString("e8c73ee0-b4d7-4d18-82de-e6694f94463f"), "2016 Mercedes-Benz E-Class", usd(62250), 10, new URL("https://www.cstatic-images.com/car-pictures/main/USC40MBC682A021001.png")),
      Good(fromString("8b098197-93a5-4663-8a09-88209ed2a3d2"), "2016 Porsche Boxster", usd(52100), 1, new URL("https://www.cstatic-images.com/car-pictures/main/USC30PRC021A021001.png")),
      Good(fromString("db207ba9-5fe4-40ad-a580-a4d14001dd92"), "2015 Aston Martin DB9", usd(203295), 5, new URL("https://www.cstatic-images.com/car-pictures/main/USC40ANC071A021001.png")),
      Good(fromString("04c3e461-d58a-4616-87cc-9c1061f2bd61"), "2016 Audi TT", usd(46400),  3,  new URL("https://www.cstatic-images.com/car-pictures/main/USC60AUC112A021001.png")),
      Good(fromString("d42f1301-e9a4-4bf3-a9be-7cdc518829cc"), "2016 BMW 228", usd(38650), 15, new URL("https://www.cstatic-images.com/car-pictures/main/USC40BMC621A021001.png")),
      Good(fromString("9b9f76e5-ae60-4b2a-b74f-0d55859d4414"), "2015 BMW 428", usd(48750), 20, new URL("https://www.cstatic-images.com/car-pictures/main/USC40BMC582A021001.png")),
      Good(fromString("75684634-20cc-4dad-9f83-0e3c2a6ebe1d"), "2016 BMW 650", usd(96200), 7, new URL("https://www.cstatic-images.com/car-pictures/main/USC60BMC281A021001.png")),
      Good(fromString("8d286895-3dcb-4d36-9c48-7afa8d2ef2bf"), "2016 Buick Cascada", usd(33065), 2, new URL("https://www.cstatic-images.com/car-pictures/main/USC60BUC181B021001.png")),
      Good(fromString("3d450324-6fd5-48b1-9866-6ad312ea2af3"), "2016 Chevrolet Corvette", usd(59400), 7, new URL("https://www.cstatic-images.com/car-pictures/main/USC40CHC371A021001.png"))
    )
  }

  def props = Props(classOf[DepotActor])

  sealed trait Query
  object Queries {
    case object GetState extends Query
  }

  sealed trait Answer
  object Answers {
    case class State(goods: List[Good]) extends Answer
  }
}

case class Good(id: UUID, name: String, price: Money, count: Int, image: URL)