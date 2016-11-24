import sbt._
import Keys._

object Dependencies {

  private val akkaV = "2.4.10"
  private val json4sV = "3.4.1"

  private val typesafeConfig = "com.typesafe" % "config" % "1.3.0"
  private val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaV
  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
  private val jodaTime = "joda-time" % "joda-time" % "2.9.4"
  private val jodaMoney = "org.joda" % "joda-money" % "0.11"
  private val json4s = "org.json4s" %% "json4s-core" % json4sV
  private val json4sExt = "org.json4s" %% "json4s-ext" % json4sV
  private val json4sJackson = "org.json4s" %% "json4s-jackson" % json4sV
  private val akkaHttpJson4s = "de.heikoseeberger" %% "akka-http-json4s" % "1.10.1"
  private val scalatest = "org.scalatest" %% "scalatest" % "2.2.6" % Test
  private val mockitoAll = "org.mockito" % "mockito-all" % "1.10.19" % Test
  private val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaV % Test
  private val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaV % Test

  val restDependencies = Seq(
    typesafeConfig,
    akkaHttp,
    akkaActor,
    jodaTime,
    jodaMoney,
    json4s,
    json4sExt,
    json4sJackson,
    akkaHttpJson4s,
    scalatest,
    mockitoAll,
    akkaHttpTestkit,
    akkaTestkit
  )
}