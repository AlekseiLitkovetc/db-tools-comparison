package ru.fsacala.dbtool.skunk

import cats.effect.*
import fs2.Stream
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object Ex04MultiParameterQuery extends IOApp {

  private case class Country(name: String, population: Int)

  private object Country {
    val decoder: Decoder[Country] =
      (varchar *: int4).to[Country]
  }

  private val countries: Query[String *: Int *: EmptyTuple, Country] =
    sql"""
      SELECT name, population
      FROM   country
      WHERE  name LIKE $varchar
      AND    population < $int4
    """.query(Country.decoder)

  def run(args: List[String]): IO[ExitCode] =
    sessionResource.use { s =>
      val stream = for {
        ps      <- Stream.eval(s.prepare(countries))
        country <- ps.stream(("U%", 2000000), 64)
        _       <- Stream.eval(IO.println(country))
      } yield ()

      stream.compile.drain.as(ExitCode.Success)
    }
}
