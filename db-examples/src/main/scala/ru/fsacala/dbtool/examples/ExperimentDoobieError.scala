package ru.fsacala.dbtool.examples

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import doobie.util.query.Query0

import java.time.LocalDate

object ExperimentDoobieError extends IOApp.Simple {

  private case class Country(name: String, population: Int)

  private val experimentDoobie: IO[Unit] = {
    def getCountries(likeValue: String) =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $likeValue
      """.query[Country]

    for {
      xa <- IO(getDoobieTransactor)
      _  <- getCountries("U%").unique.transact(xa) // Exactly one row was expected, but more were returned
      // _  <- getCountries("U%").option.transact(xa) // Expected at most one result, more returned
    } yield ()
  }

  override def run: IO[Unit] =
    experimentDoobie

}
