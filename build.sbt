import sbt.Keys._
import Dependencies._

lazy val buildSettings = Seq(
  organization := "com.github.sonenko",
  version := "0.0.1",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-feature", "-language:postfixOps", "-deprecation",
    "-Ywarn-dead-code", "-Xfatal-warnings", "-unchecked"),
  coverageEnabled := false
)

lazy val rest = (project in file("rest"))
  .settings(buildSettings: _*)
  .settings(
    description := "Rest service for Shopping basket",
    name := "Shopping Basket Rest",
    assemblyJarName in assembly := "ShoppingBasketRest.jar",
    mainClass in assembly := Some("com.github.sonenko.shoppingbasket.Main"),
    libraryDependencies ++= restDependencies
  )

lazy val root = (project in file(".")).
  aggregate(rest)
