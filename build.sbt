
name := "storage-example"

organization := "com.freedomandy"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "azure" at "http://mvnrepository.com/artifact/com.microsoft.azure"
)

libraryDependencies ++= {
  val AkkaHttpVersion   = "10.0.0"
  Seq(
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "com.typesafe" % "config" % "1.3.0",
    "com.microsoft.azure" % "azure-storage" % "4.0.0",
    "com.microsoft.azure" % "azure-core" % "0.9.3",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.apache.httpcomponents" % "httpclient" % "4.5.1",
    "org.json4s" % "json4s-jackson_2.11" % "3.5.0"
  )
}

enablePlugins(JavaAppPackaging)