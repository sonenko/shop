import sbt.Keys._

name := "Shop"

description := "Simple Shop that able to scale in minutes :)"

version := "0.0.1"

scalaVersion := "2.11.8"

scalacOptions ++= List(
  "-feature",
  "-language:postfixOps",
  "-deprecation",
  "-Ywarn-dead-code"
)

assemblyJarName in assembly := "Shop.jar"
mainClass in assembly := Some("com.github.sonenko.shoppingbasket.Main")

resolvers ++= List(
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "Bintray sbt plugin releases" at "http://dl.bintray.com/sbt/sbt-plugin-releases/"
)

val akkaV = "2.4.10"
val json4sV = "3.4.1"

libraryDependencies ++= List(
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-persistence" % akkaV,
  "com.typesafe.akka" %% "akka-remote" % akkaV,
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "joda-time" % "joda-time" % "2.9.4",
  "org.joda" % "joda-money" % "0.11",
  "org.json4s" %% "json4s-core" % json4sV,
  "org.json4s" %% "json4s-ext" % json4sV,
  "org.json4s" %% "json4s-jackson" % json4sV,
  "de.heikoseeberger" %% "akka-http-json4s" % "1.10.1",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
)