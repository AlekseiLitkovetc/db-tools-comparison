package ru.fsacala.dbtool.examples

import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import java.time.LocalDate

/** These experiments check proposals described on the [[https://typelevel.org/skunk/tutorial/Query.html#experiment Skunk page]]
  */
object Experiments extends IOApp.Simple {

  private case class Country(name: String, population: Int)

  private object Country {
    val decoder: Decoder[Country] = (varchar *: int4).to[Country]
  }

  /** Try to run the extended query via [[skunk.Session#execute]], or the simple query via [[skunk.Session#prepare]].
    * Note that in the latter case you will need to pass the value Void as an argument.
    */
  private val experiment01: IO[Unit] = {
    val currentDateQuery: Query[Void, LocalDate] =
      sql"select current_date".query(date)

    val countries: Query[String, Country] =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $varchar
      """.query(Country.decoder)

    sessionResourceSkunk.use { s =>
      for {
        // Extended query via skunk.Session#execute
        cs <- s.execute(countries)("U%")
        _  <- IO.println(s"Available countries are: $cs")

        // Simple query via skunk.Session#prepare
        date <- s.prepare(currentDateQuery).flatMap(_.unique(Void))
        _    <- IO.println(s"The current date is $date")
      } yield ()
    }
  }

  /** Add/remove/change encoders and decoders. 
    * Do various things to make the queries fail. 
    * Which kinds of errors are detected at compile-time vs. runtime?
    * 
    * Answer - at compile-time we face errors related to type errors and at runtime we face errors related to DB errors 
    */
  private val experiment02: IO[Unit] = {
    // Type Mismatch Error
    // val compileTimeErrorExample: Query[String, Country] =
    //   sql"""
    //     SELECT name, population
    //     FROM   country
    //     WHERE  name LIKE $varchar
    //   """.query(varchar)

    val countries =
      sql"""
        SELECT name, population
        FROM   country
        WHERE  name LIKE $varchar
      """.query(Country.decoder)

    sessionResourceSkunk.use { s =>
      for {
        ps <- s.prepare(countries)
        _  <- ps.unique("U%") // Exactly one row was expected, but more were returned
        // _  <- ps.option("U%") // Expected at most one result, more returned
      } yield ()
    }
  }

  /** Add more fields to Country and more colums to the query; or add more parameters.
    * You will need to consult the [[https://typelevel.org/skunk/reference/SchemaTypes.html Schema Types]] 
    * reference to find the encoders/decoders you need.
    */
  private val experiment03: IO[Unit] = {
    val query: Query[Void, (String, Int, Boolean)] =
      sql"SELECT 'a', 1, true"
        .query(text *: int4 *: bool)

    sessionResourceSkunk.use { s =>
      for {
        res <- s.unique(query)
        _   <- IO.println(s"The result is $res")
      } yield ()
    }
  }

  /** Experiment with the treatment of nullable columns.
    * You need to add .opt to encoders/decoders (int4.opt for example) to indicate nullability.
    * Keep in mind that for interpolated encoders you'll need to write ${int4.opt}.
    */
  private val experiment04: IO[Unit] = {
    val query: Query[Void, Option[Int]] =
      sql"SELECT null::int"
        .query(int4.opt)

    sessionResourceSkunk.use { s =>
      for {
        res <- s.unique(query)
        _   <- IO.println(s"The result is $res")
      } yield ()
    }
  }

  override def run: IO[Unit] =
    experiment01

}
