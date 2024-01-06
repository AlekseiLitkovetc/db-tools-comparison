package ru.fsacala.dbtool.skunk

import cats.effect.*
import ru.fsacala.dbtool.skunk.Ex02MultiColumnQuery.countriesWithCustomDecoder2
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.LocalDate

object Ex02MultiColumnQuery extends IOApp {

  // Multi column queries

  private val countries: Query[Void, String ~ Int] =
    sql"SELECT name, population FROM country".query(varchar ~ int4)

  private val countriesWithMapping: Query[Void, Country] =
    sql"SELECT name, population FROM country"
      .query(varchar ~ int4)
      .map { case n ~ p => Country(n, p) }

  // Adding model and defining decoders

  private case class Country(name: String, population: Int)

  private object Country {
    val decoder: Decoder[Country] =
      (varchar ~ int4).map { case (n, p) => Country(n, p) }

    val decoder2: Decoder[Country] =
      (varchar *: int4).to[Country]
  }

  private val countriesWithCustomDecoder: Query[Void, Country] =
    sql"SELECT name, population FROM country"
      .query(Country.decoder)

  private val countriesWithCustomDecoder2: Query[Void, Country] =
    sql"SELECT name, population FROM country"
      .query(Country.decoder2)

  def run(args: List[String]): IO[ExitCode] =
    sessionResource.use { s =>
      for {
        cs <- s.execute(countries)
        _          <- IO.println(s"There are countries with population info: $cs")

        csWithMapping <- s.execute(countriesWithMapping)
        _          <- IO.println(s"There are countries with population info (using mapping): $csWithMapping")

        csWithCustomDecoder <- s.execute(countriesWithCustomDecoder)
        _          <- IO.println(s"There are countries with population info (using decoder): $csWithCustomDecoder")

        csWithCustomDecoder2 <- s.execute(countriesWithCustomDecoder2)
        _          <- IO.println(s"There are countries with population info (using decoder 2): $csWithCustomDecoder2")
      } yield ExitCode.Success
    }
}
