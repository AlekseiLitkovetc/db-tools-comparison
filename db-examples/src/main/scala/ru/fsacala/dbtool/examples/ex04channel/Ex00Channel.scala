package ru.fsacala.dbtool.examples.ex04channel

import cats.effect.*
import cats.syntax.all.*
import ru.fsacala.dbtool.examples.sessionResourceSkunk
import ru.fsacala.dbtool.examples.getDoobieTransactor
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.LocalDate

object Ex00Channel extends IOApp.Simple {

  val currentDateQuery: Query[Void, LocalDate] =
    sql"SELECT current_date"
      .query(date)

  private def withTrigger(s: Session[IO]): Resource[IO, Unit] = {
    val createTableCommand =
      sql"""
        CREATE TABLE person(
          first_name VARCHAR(500) NOT NULL,
          last_name VARCHAR(500) NOT NULL,
          age INT NOT null
        )
      """.command

    val dropTableCommand =
      sql"DROP TABLE person".command

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
      """.command

    val dropFunctionCommand =
      sql"DROP FUNCTION person_on_insert".command

    val createTriggerCommand =
      sql"""
        CREATE TRIGGER person_trigger
        AFTER INSERT ON person
        FOR EACH ROW EXECUTE PROCEDURE person_on_insert()
      """.command

    val alloc =
      s.execute(createTableCommand) >>
        s.execute(createFunctionCommand) >>
        s.execute(createTriggerCommand)

    val free =
      s.execute(dropTableCommand) >>
        s.execute(dropFunctionCommand)

    Resource.make(alloc.void)(_ => free.void)
  }

  def run: IO[Unit] =
    sessionResourceSkunk
      .flatTap(withTrigger)
      .use {
        _.channel(id"person_inserts")
          .listen(1024)
          .evalTap(IO.println)
          .compile
          .drain
      }

}
