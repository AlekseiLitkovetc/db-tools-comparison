package ru.fsacala.dbtool.skunk.ex01queries

import cats.effect.*
import cats.effect.unsafe.implicits.global
import natchez.Trace.Implicits.noop
import org.openjdk.jmh.annotations.*
import fs2.Stream

import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.sql.DriverManager

import skunk.*
import doobie.*

object SkunkBenchmarkState2 {
  @State(Scope.Benchmark)
  val sessionResource: Resource[IO, skunk.Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "db-tools-comparison",
      password = Some("password")
    )
}

object DoobieBenchmarkState2 {
  @State(Scope.Benchmark)
  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5433/db-tools-comparison",
    user = "postgres",
    password = "password",
    logHandler = None
  )
}

@BenchmarkMode(Array(Mode.Throughput, Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(iterations = 15, timeUnit = TimeUnit.SECONDS, time = 3)
@Warmup(iterations = 15, timeUnit = TimeUnit.SECONDS, time = 3)
@Fork(value = 1)
@Threads(value = 1)
@OperationsPerInvocation(1000)
class Bm02SelectBenchmark {
  import SkunkBenchmarkState2.*
  import DoobieBenchmarkState2.*

  def jdbcSelect(n: Int): Int = {
    Class.forName("org.postgresql.Driver")
    val co = DriverManager.getConnection("jdbc:postgresql://localhost:5433/db-tools-comparison", "postgres", "password")
    try {
      co.setAutoCommit(false)
      val ps = co.prepareStatement("select a.name, b.name, co.name from country a, country b, country co limit ?")
      try {
        ps.setInt(1, n)
        val rs = ps.executeQuery
        try {
          val accum = List.newBuilder[(String, String, String)]
          while (rs.next) {
            val a = rs.getString(1); rs.wasNull
            val b = rs.getString(2); rs.wasNull
            val c = rs.getString(3); rs.wasNull
            accum += ((a, b, c))
          }
          accum.result().length
        } finally rs.close
      } finally ps.close
    } finally {
      co.commit()
      co.close()
    }
  }

  def skunkSelect(n: Int): Int = {
    import skunk.codec.all.*
    import skunk.implicits.*

    sessionResource
      .use { s =>
        val countries = sql"select a.name, b.name, c.name from country a, country b, country c limit $int4"
          .query(varchar *: varchar *: varchar)

        Stream
          .eval(s.prepare(countries))
          .flatMap(_.stream(n, 64))
          .compile
          .toList
          .map(_.length)
      }
      .unsafeRunSync()
  }

  def doobieSelect(n: Int): Int = {
    import doobie.implicits.*

    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String, String, String)]
      .stream
      .compile
      .toList
      .transact(xa)
      .map(_.length)
      .unsafeRunSync()
  }

  @Benchmark
  def runEmptyMethod(): Unit = {
    // this method was intentionally left blank
  }

  @Benchmark
  def selectViaJdbc: Int = jdbcSelect(1000)

  @Benchmark
  def selectViaSkunk: Int = skunkSelect(1000)

  @Benchmark
  def selectViaDoobie: Int = doobieSelect(1000)

}
