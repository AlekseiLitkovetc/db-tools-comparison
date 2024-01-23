package ru.fsacala.dbtool.examples.ex01queries

import cats.effect.*
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor

import java.time.LocalDate

object Ex02MultiColumnQuery extends IOApp.Simple {

  private case class Country(name: String, population: Int)

  private val skunkExample: IO[Unit] = {
    import skunk.*
    import skunk.codec.all.*
    import skunk.implicits.*

    // Defining decoders

    val decoder: Decoder[Country] =
      (varchar ~ int4)
        .map { case (n, p) => Country(n, p) }

    val decoder2: Decoder[Country] =
      (varchar *: int4)
        .to[Country]

    // Multi column queries

    val countries: Query[Void, String ~ Int] =
      sql"SELECT name, population FROM country"
        .query(varchar ~ int4)

    val countriesWithMapping: Query[Void, Country] =
      sql"SELECT name, population FROM country"
        .query(varchar ~ int4)
        .map { case n ~ p => Country(n, p) }

    val countriesWithCustomDecoder: Query[Void, Country] =
      sql"SELECT name, population FROM country"
        .query(decoder)

    val countriesWithCustomDecoder2: Query[Void, Country] =
      sql"SELECT name, population FROM country"
        .query(decoder2)

    sessionResourceSkunk.use { s =>
      for {
        cs <- s.execute(countries)
        _  <- IO.println(s"[Skunk] There are countries with population info: $cs")

        csWithMapping <- s.execute(countriesWithMapping)
        _             <- IO.println(s"[Skunk] There are countries with population info (using mapping): $csWithMapping")

        csWithCustomDecoder <- s.execute(countriesWithCustomDecoder)
        _                   <- IO.println(s"[Skunk] There are countries with population info (using decoder): $csWithCustomDecoder")

        csWithCustomDecoder2 <- s.execute(countriesWithCustomDecoder2)
        _                    <- IO.println(s"[Skunk] There are countries with population info (using decoder 2): $csWithCustomDecoder2")
      } yield ()
    }
  }

  private val doobieExample: IO[Unit] = {
    import doobie.*
    import doobie.implicits.*
    import doobie.postgres.implicits.*
    import doobie.syntax.*

    // Multi column queries

    val countries: Query0[(String, Int)] =
      sql"SELECT name, population FROM country"
        .query[(String, Int)]

    val countriesWithMapping: Query0[Country] =
      sql"SELECT name, population FROM country"
        .query[Country]

    for {
      xa <- IO(getDoobieTransactor)

      cs <- countries.to[List].transact(xa)
      _  <- IO.println(s"[Doobie] There are countries with population info: $cs")

      csWithMapping <- countriesWithMapping.to[List].transact(xa)
      _             <- IO.println(s"[Doobie] There are countries with population info (using mapping): $csWithMapping")
    } yield ()
  }

  def run: IO[Unit] =
    skunkExample >> doobieExample

}
