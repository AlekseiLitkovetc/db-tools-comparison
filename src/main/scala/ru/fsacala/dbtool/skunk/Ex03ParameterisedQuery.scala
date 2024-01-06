package ru.fsacala.dbtool.skunk

import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import fs2.Stream

object Ex03ParameterisedQuery extends IOApp {

  private case class Country(name: String, population: Int)

  private object Country {
    val decoder: Decoder[Country] =
      (varchar *: int4).to[Country]
  }

  private val countries: Query[String, Country] =
    sql"""
      SELECT name, population
      FROM   country
      WHERE  name LIKE $varchar
    """.query(Country.decoder)

  def run(args: List[String]): IO[ExitCode] =
    sessionResource.use { s =>
      val stream = for {
        ps      <- Stream.eval(s.prepare(countries))
        country <- ps.stream("U%", 64)
        _       <- Stream.eval(IO.println(country))
      } yield ()

      stream.compile.drain.as(ExitCode.Success)
    }
}
