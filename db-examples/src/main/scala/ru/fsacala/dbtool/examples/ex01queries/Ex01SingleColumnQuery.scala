package ru.fsacala.dbtool.examples.ex01queries

import cats.effect.*
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor

object Ex01SingleColumnQuery extends IOApp.Simple {

  private val skunkExample: IO[Unit] = {
    import skunk.*
    import skunk.codec.all.*
    import skunk.implicits.*

    val countries: Query[Void, String] =
      sql"SELECT name FROM country"
        .query(varchar)

    sessionResourceSkunk.use { s =>
      for {
        cs <- s.execute(countries)
        _  <- IO.println(s"[Skunk] There are fetched countries: $cs")
      } yield ()
    }
  }

  private val doobieExample: IO[Unit] = {
    import doobie.*
    import doobie.implicits.*
    import doobie.postgres.implicits.*
    import doobie.syntax.*
    import doobie.util.query.Query0

    val countries: Query0[String] =
      sql"SELECT name FROM country"
        .query[String]

    for {
      xa <- IO(getDoobieTransactor)
      cs <- countries.to[List].transact(xa)
      _  <- IO.println(s"[Doobie] There are fetched countries: $cs")
    } yield ()
  }

  def run: IO[Unit] =
    skunkExample >> doobieExample

}
