import Dependencies.*

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / Compile / run / fork := true

lazy val examples = project
  .in(file("db-examples"))
  .withId("db-examples")
  .settings(
    libraryDependencies += catsEffect,
    libraryDependencies += skunk,
    libraryDependencies += fs2
  )

lazy val benchmarks = project
  .in(file("db-benchmarks"))
  .withId("db-benchmarks")
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies += catsEffect,
    libraryDependencies += skunk,
    libraryDependencies ++= doobie,
    libraryDependencies += fs2
  )
