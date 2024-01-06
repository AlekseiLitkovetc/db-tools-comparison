package ru.fsacala.dbtool.skunk

import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.LocalDate

object Ex00CurrentDateQuery extends IOApp {

  private val currentDateQuery: Query[Void, LocalDate] =
    sql"select current_date".query(date)

  def run(args: List[String]): IO[ExitCode] =
    sessionResource.use { s =>
      for {
        date <- s.unique(currentDateQuery)
        _    <- IO.println(s"The current date is $date.")
      } yield ExitCode.Success
    }
}
