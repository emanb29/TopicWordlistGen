name := "TopicWordlistGen"

version := "0.1"

scalaVersion := "2.12.8"
scalacOptions += "-Ypartial-unification"

val circeVersion = "0.10.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.2.0",
  "com.softwaremill.sttp" %% "core" % "1.5.12",
  "com.softwaremill.sttp" %% "circe" % "1.5.12",
)