name := "LearnScala"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe.akka" %% "akka-actor" % "2.6.12",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.12",
  "com.ning" % "async-http-client" % "1.7.19",
  "org.jsoup" % "jsoup" % "1.8.1",
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-cluster
  "com.typesafe.akka" %% "akka-cluster" % "2.6.12"

)
