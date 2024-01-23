package ru.fsacala.dbtool.examples.ex01queries

import cats.effect.*
import fs2.Stream
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor

object Ex04MultiParameterQuery extends IOApp.Simple {

  private case class Country(name: String, population: Int)

  private val skunkExample: IO[Unit] = {
    import skunk.*
    import skunk.codec.all.*
    import skunk.implicits.*

    val decoder: Decoder[Country] =
      (varchar *: int4)
        .to[Country]

    val countries: Query[String *: Int *: EmptyTuple, Country] =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $varchar
        AND    population < $int4
      """.query(decoder)

    sessionResourceSkunk.use { s =>
      val stream = for {
        ps      <- Stream.eval(s.prepare(countries))
        country <- ps.stream(("U%", 2000000), 64)
        _       <- Stream.eval(IO.println(country))
      } yield ()

      stream.compile.drain
    }
  }

  private val doobieExample: IO[Unit] = {
    import doobie.*
    import doobie.implicits.*
    import doobie.postgres.implicits.*
    import doobie.syntax.*

    def getCountries(likeValue: String, maxPopulation: Int): Query0[Country] =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $likeValue
        AND    population < $maxPopulation
      """.query[Country]

    val stream = for {
      xa      <- Stream.eval(IO(getDoobieTransactor))
      country <- getCountries("U%", 2000000).stream.transact(xa)
      _       <- Stream.eval(IO.println(country))
    } yield ()

    stream.compile.drain
  }

  def run: IO[Unit] =
    skunkExample >> doobieExample

}
