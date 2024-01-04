import Dependencies.Versions.*
import sbt.*

object Dependencies {

  object Versions {
    lazy val catsEffectVersion = "3.5.2"
    lazy val skunkVersion      = "0.6.2"
  }

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

  lazy val skunk = "org.tpolecat" %% "skunk-core" % skunkVersion
}
