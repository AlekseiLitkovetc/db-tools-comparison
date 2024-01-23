package ru.fsacala.dbtool.examples.ex01queries

import cats.effect.*
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor

import java.time.LocalDate

object Ex00CurrentDateQuery extends IOApp.Simple {

  private val skunkExample: IO[Unit] = {
    import skunk.*
    import skunk.codec.all.*
    import skunk.implicits.*

    val currentDateQuery: Query[Void, LocalDate] =
      sql"SELECT current_date"
        .query(date)

    sessionResourceSkunk.use { s =>
      for {
        date <- s.unique(currentDateQuery)
        _    <- IO.println(s"[Skunk] The current date is $date.")
      } yield ()
    }
  }

  private val doobieExample: IO[Unit] = {
    import doobie.*
    import doobie.implicits.*
    import doobie.postgres.implicits.*
    import doobie.syntax.*
    import doobie.util.query.Query0


    val currentDateQuery: Query0[LocalDate] =
      sql"SELECT current_date"
        .query[LocalDate]

    for {
      xa   <- IO(getDoobieTransactor)
      date <- currentDateQuery.unique.transact(xa)
      _    <- IO.println(s"[Doobie] The current date is $date.")
    } yield ()
  }

  def run: IO[Unit] =
    skunkExample >> doobieExample

}
