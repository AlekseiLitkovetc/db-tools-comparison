import Dependencies.Versions._
import sbt._

object Dependencies {

  object Versions {
    lazy val catsEffectVersion = "3.5.2"
    lazy val skunkVersion      = "0.6.2"
    lazy val doobieVersion     = "1.0.0-RC5"
    lazy val fs2Version        = "3.9.3"
    lazy val doobieVersion     = "1.0.0-RC5"
    lazy val otel4sVerson            = "0.4.0"
  }

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

  lazy val skunk = "org.tpolecat" %% "skunk-core" % skunkVersion

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-postgres-circe"
  ).map(_ % doobieVersion)

  lazy val fs2 = "co.fs2" %% "fs2-core" % fs2Version

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-postgres-circe"
  ).map(_ % doobieVersion)

  lazy val otel4s = "org.typelevel" %% "otel4s-java" % otel4sVerson
}
