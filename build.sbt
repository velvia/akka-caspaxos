organization := "org.velvia"

scalaVersion := "2.11.8"

val akkaVersion       = "2.4.19" // akka-http/akka-stream compat. TODO when kamon-akka-remote is akka 2.5.4 compat

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.typesafe.akka"    %% "akka-slf4j"        % akkaVersion,
  "com.typesafe.akka"    %% "akka-cluster"      % akkaVersion,
  "com.typesafe.akka"    %% "akka-testkit"      % akkaVersion % Test,
  "com.typesafe.akka"    %% "akka-multi-node-testkit" % akkaVersion % Test
)