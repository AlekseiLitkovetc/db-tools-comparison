package ru.fsacala.dbtool.examples.ex01queries

import cats.effect.*
import fs2.Stream
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor

object Ex03ParameterisedQuery extends IOApp.Simple {

  private case class Country(name: String, population: Int)

  private val skunkExample: IO[Unit] = {
    import skunk.*
    import skunk.codec.all.*
    import skunk.implicits.*

    val decoder: Decoder[Country] =
      (varchar *: int4)
        .to[Country]

    val countries: Query[String, Country] =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $varchar
      """.query(decoder)

    sessionResourceSkunk.use { s =>
      val stream = for {
        ps      <- Stream.eval(s.prepare(countries))
        country <- ps.stream("U%", 64)
        _       <- Stream.eval(IO.println(country))
      } yield ()

      IO.println("[Skunk] Countries:") >> stream.compile.drain
    }
  }

  private val doobieExample: IO[Unit] = {
    import doobie.*
    import doobie.implicits.*
    import doobie.postgres.implicits.*
    import doobie.syntax.*
    import doobie.util.query.Query0

    def getCountries(likeValue: String): Query0[Country] =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $likeValue
      """.query[Country]

    val stream = for {
      xa      <- Stream.eval(IO(getDoobieTransactor))
      country <- getCountries("U%").stream.transact(xa)
      _       <- Stream.eval(IO.println(country))
    } yield ()

    IO.println("[Doobie] Countries:") >> stream.compile.drain
  }

  def run: IO[Unit] =
    skunkExample >> doobieExample

}
