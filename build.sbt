import Dependencies.*

scalaVersion := "3.3.1"
version      := "0.1.0-SNAPSHOT"

lazy val root = project
  .in(file("."))
  .settings()
  .settings(
    libraryDependencies += catsEffect,
    libraryDependencies += skunk
  )
