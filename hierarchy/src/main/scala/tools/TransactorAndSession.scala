package tools

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import natchez.Trace.Implicits.noop
import skunk.*

import java.sql.DriverManager

object TransactorAndSession {

  private val poolSize = 8
  private val driver   = "org.postgresql.Driver"
  private val url      = "jdbc:postgresql://localhost:5432/hierarchy"
  private val host     = "localhost"
  private val port     = 5432
  private val db       = "hierarchy"
  private val user     = "vsevolod66rus"
  private val password = "***"

  val transactorResource: Resource[IO, Transactor[IO]] = for {
    connectEC  <- ExecutionContexts.fixedThreadPool[IO](poolSize)
    transactor <- HikariTransactor.newHikariTransactor[IO](
                    driver,
                    url,
                    user,
                    password,
                    connectEC
                  )
  } yield transactor

  val sessionResource: Resource[IO, Session[IO]] =
    Session.single(
      host = host,
      port = port,
      user = user,
      database = db,
      password = Some(password)
    )
}
