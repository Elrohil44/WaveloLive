name := "server"

version := "1.0"

scalaVersion := "2.12.2"

enablePlugins(JavaAppPackaging)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.6"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.6"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"