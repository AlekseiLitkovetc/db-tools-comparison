package ru.fsacala.dbtool.examples.ex04channel

import cats.effect.*
import cats.effect.implicits.*
import cats.syntax.all.*
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import doobie.util.query.Query0
import fs2.{Pipe, Stream}
import fs2.Stream.*

import java.time.LocalDate
import scala.concurrent.duration.*
import org.postgresql.PGNotification

object Ex01ChannelDoobie extends IOApp.Simple {

  private def withTrigger(xa: Transactor[IO]): Resource[IO, Unit] = {
    val createTableCommand =
      sql"""
        CREATE TABLE person(
          first_name VARCHAR(500) NOT NULL,
          last_name VARCHAR(500) NOT NULL,
          age INT NOT null
        )
      """.update.run

    val dropTableCommand =
      sql"DROP TABLE person".update.run

    val createFunctionCommand =
      sql"""
        CREATE OR REPLACE FUNCTION person_on_insert() RETURNS TRIGGER AS $$$$
        BEGIN
          PERFORM (
            WITH payload("first_name", "last_name", "age") AS (
              SELECT NEW.first_name, NEW.last_name, NEW.age
            )
            SELECT pg_notify('person_inserts', row_to_json(payload) :: TEXT)
            FROM payload
          );
          RETURN NULL;
        END
        $$$$ LANGUAGE 'plpgsql'
      """.update.run

    val dropFunctionCommand =
      sql"DROP FUNCTION person_on_insert".update.run

    val createTriggerCommand =
      sql"""
        CREATE TRIGGER person_trigger
        AFTER INSERT ON person
        FOR EACH ROW EXECUTE PROCEDURE person_on_insert()
      """.update.run

    val alloc =
      createTableCommand >>
        createFunctionCommand >>
        createTriggerCommand

    val free =
      dropTableCommand >>
        dropFunctionCommand

    Resource.make(alloc.transact(xa).void)(_ => free.transact(xa).void)
  }

  // A resource that listens on a channel and unlistens when we're done.
  private def channel(name: String): Resource[ConnectionIO, Unit] =
    Resource.make(PHC.pgListen(name) *> HC.commit)(_ => PHC.pgUnlisten(name) *> HC.commit)

  // Stream of PGNotifications on the specified channel, polling at the specified
  // rate. Note that this stream, when run, will commit the current transaction.
  private def notificationStream(
      channelName: String,
      pollingInterval: FiniteDuration,
      xa: Transactor[IO]
  ): Stream[IO, PGNotification] = {
    val inner: Pipe[ConnectionIO, FiniteDuration, PGNotification] = ticks =>
      for {
        _  <- resource(channel(channelName))
        _  <- ticks
        ns <- eval(PHC.pgGetNotifications <* HC.commit)
        n  <- emits(ns)
      } yield n
    awakeEvery[IO](pollingInterval).through(inner.transact(xa))
  }

  def run: IO[Unit] =
    Resource
      .eval(IO(getDoobieTransactor))
      .flatTap(withTrigger)
      .use { xa =>
        val stream = notificationStream(
          channelName = "person_inserts",
          pollingInterval = 1.second,
          xa = xa
        )

        stream
          .evalTap(n => IO.println(n.getParameter))
          .compile
          .drain
      }

}
