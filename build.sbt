import sbt._
// scalafmt: { align.preset = most, danglingParentheses.preset = false }
name := "ziokoban"

version := "0.1.0"

scalaVersion := "2.13.3"
Global / scalaVersion := "2.13.3"
ThisBuild / scalaVersion := "2.13.3" 

organization := "nl.itvanced"
maintainer := "rob.vermazeren@itvanced.nl"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

enablePlugins(JavaAppPackaging, UniversalPlugin)

scalacOptions ++= Seq(
  "-deprecation"
  , "-unchecked"
  , "-encoding", "UTF-8"
  , "-Xlint:-unused"
  , "-Xverify"
  , "-feature"
 // ,"-Ypartial-unification"
  ,"-Xfatal-warnings"
  , "-language:_"
)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.8", "-target", "1.8")

val CatsVersion = "2.0.0"
val ZIOVersion  = "1.0.1"
val ZIOConfigVersion  = "1.0.0-RC26"
val CirceVersion = "0.12.3"

libraryDependencies ++= Seq(
  // ZIO
  "dev.zio"       %% "zio"                 % ZIOVersion,
  "dev.zio"       %% "zio-config"          % ZIOConfigVersion,
  "dev.zio"       %% "zio-config-refined"  % ZIOConfigVersion,
  "dev.zio"       %% "zio-config-typesafe" % ZIOConfigVersion,
  // Cats 
  "org.typelevel" %% "cats-core" % CatsVersion,
  // XML reading
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  // JSON support
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  // Console handling
  "org.fusesource.jansi" % "jansi" % "1.17.1", // "1.18",
  "org.jline"            % "jline" % "3.10.0", //  "3.16.0",
  // File handling 
  "com.lihaoyi"    %% "os-lib"    % "0.7.1",
  // Utility
  "org.scalactic"  %% "scalactic"  % "3.0.8",
  // Testing
  "dev.zio"        %% "zio-test"     % ZIOVersion % "test",
  "dev.zio"        %% "zio-test-sbt" % ZIOVersion % "test",
  "org.scalacheck" %% "scalacheck"   % "1.14.1"   % "test",
  "org.scalatest"  %% "scalatest"    % "3.0.8"    % "test"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

resolvers ++= Seq(
  "Typesafe Snapshots"          at "https://repo.typesafe.com/typesafe/snapshots/",
  "Secured Central Repository"  at "https://repo1.maven.org/maven2",
  Resolver.sonatypeRepo("snapshots")
)

fork in run := true

mainClass in Compile := Some("nl.itvanced.ziokoban.ZiokobanApp")
