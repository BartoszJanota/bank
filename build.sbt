name := "bank"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.18",
    "com.typesafe.akka" %% "akka-http" % "10.0.6",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.json4s" %% "json4s-native" % "3.5.2",
    "org.json4s" %% "json4s-ext" % "3.5.2",
    "de.heikoseeberger" %% "akka-http-json4s" % "1.16.0",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10",
    "org.scalactic" %% "scalactic" % "3.0.1",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )
}

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
