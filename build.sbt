import sbt._

name := "ziokoban"

version := "0.1.0"

scalaVersion := "2.12.8"

organization := "nl.itvanced"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

enablePlugins(JavaAppPackaging)

scalacOptions ++= Seq(
  "-deprecation"
  , "-unchecked"
  , "-encoding", "UTF-8"
  , "-Xlint:-unused"
  , "-Xverify"
  , "-feature"
  ,"-Ypartial-unification"
  ,"-Xfatal-warnings"
  , "-language:_"
)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.8", "-target", "1.8")

val CatsVersion = "1.6.1"
val ZIOVersion  = "1.0.0-RC10-1"

libraryDependencies ++= Seq(
  // ZIO
  "dev.zio"       %% "zio" % ZIOVersion,
  // Cats 
  "org.typelevel" %% "cats-core" % CatsVersion,
  // PureConfig
  "com.github.pureconfig" %% "pureconfig" % "0.11.0",   
  // XML reading
  "com.lucidchart" %% "xtract" % "2.0.1",
  // Console handling
  "org.fusesource.jansi" % "jansi" % "1.17.1",
  "org.jline"            % "jline" % "3.10.0",
  // Testing
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalatest"  %% "scalatest"  % "3.0.1"  % "test"
)

resolvers ++= Seq(
  "Typesafe Snapshots"          at "http://repo.typesafe.com/typesafe/snapshots/",
  "Secured Central Repository"  at "https://repo1.maven.org/maven2",
  Resolver.sonatypeRepo("snapshots")
)

fork in run := true

mainClass in Compile := Some("nl.itvanced.ZiokobanApp")
