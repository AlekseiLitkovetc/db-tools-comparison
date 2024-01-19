package ru.fsacala.dbtool.skunk

import cats.effect.*
import natchez.Trace.Implicits.noop
import skunk.*

val sessionResource: Resource[IO, Session[IO]] =
  Session.single(
    host = "localhost",
    port = 5433,
    user = "postgres",
    database = "frm-comparison-db",
    password = Some("password")
  )

val sessionPool: Resource[IO, Resource[IO, Session[IO]]] =
  Session
    .pooled[IO](
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "frm-comparison-db",
      password = Some("my-pass"),
      max = 10
    )
