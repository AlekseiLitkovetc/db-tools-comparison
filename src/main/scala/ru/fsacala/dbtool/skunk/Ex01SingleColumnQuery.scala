package ru.fsacala.dbtool.skunk

import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object Ex01SingleColumnQuery extends IOApp {

  private val countries: Query[Void, String] =
    sql"SELECT name FROM country".query(varchar)

  def run(args: List[String]): IO[ExitCode] =
    sessionResource.use { s =>
      for {
        cs <- s.execute(countries)
        _  <- IO.println(s"There are fetched countries: $cs")
      } yield ExitCode.Success
    }
}
