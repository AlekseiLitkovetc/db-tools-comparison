package ru.fsacala.dbtool.examples

import cats.effect.*
import natchez.Trace.Implicits.noop
import skunk.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import doobie.util.transactor.Transactor

val sessionResourceSkunk: Resource[IO, Session[IO]] =
  Session.single(
    host = "localhost",
    port = 5433,
    user = "postgres",
    database = "db-tools-comparison",
    password = Some("password")
  )

val sessionResourceSkunkWithDebug: Resource[IO, Session[IO]] =
  Session.single(
    host = "localhost",
    port = 5433,
    user = "postgres",
    database = "db-tools-comparison",
    password = Some("password"),
    debug = true
  )  

val sessionPoolSkunk: Resource[IO, Resource[IO, Session[IO]]] =
  Session
    .pooled[IO](
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "db-tools-comparison",
      password = Some("my-pass"),
      max = 10
    )

def getDoobieTransactor: Transactor[IO] =
  Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5433/db-tools-comparison",
    user = "postgres",
    password = "password",
    logHandler = None
  )
