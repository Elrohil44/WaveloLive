
name := "server"

version := "1.0"

scalaVersion := "2.12.2"

enablePlugins(JavaAppPackaging)


libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.0"
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.4"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.6"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.6"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"
libraryDependencies += "org.postgresql" % "postgresql" % "42.1.1"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.3" % "test"
