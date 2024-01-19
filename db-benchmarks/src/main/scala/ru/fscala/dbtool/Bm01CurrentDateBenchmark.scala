package ru.fsacala.dbtool.skunk.ex01queries

import cats.effect.*
import cats.effect.unsafe.implicits.global
import natchez.Trace.Implicits.noop
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import java.time.LocalDate

@State(Scope.Benchmark)
class SkunkBenchmarkState {
  import skunk.*
  import skunk.codec.all.*
  import skunk.implicits.*

  val sessionResource: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "frm-comparison-db",
      password = Some("password")
    )

  val currentDateQuery: Query[Void, LocalDate] =
    sql"select current_date".query(date)

  val currentDateF: IO[LocalDate] = sessionResource.use(_.unique(currentDateQuery))
}

@State(Scope.Benchmark)
class DoobieBenchmarkState {
  import doobie.*
  import doobie.implicits.*
  import doobie.postgres.implicits.*
  import doobie.syntax.*

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5433/frm-comparison-db",
    user = "postgres",
    password = "password",
    logHandler = None
  )

  val currentDateQuery: Query0[LocalDate] =
    sql"select current_date".query[LocalDate]

  val currentDateF: IO[LocalDate] = currentDateQuery.unique.transact(xa)
}

@BenchmarkMode(Array(Mode.Throughput, Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(iterations = 15, timeUnit = TimeUnit.SECONDS, time = 3)
@Warmup(iterations = 15, timeUnit = TimeUnit.SECONDS, time = 3)
@Fork(value = 1)
@Threads(value = 1)
class Bm01CurrentDateBenchmark {

  @Benchmark
  def runEmptyMethod(): Unit = {
    // this method was intentionally left blank
  }

  @Benchmark
  def getSkunkCurrentDate(skunkState: SkunkBenchmarkState): LocalDate =
    skunkState.currentDateF.unsafeRunSync()

  @Benchmark
  def getDoobieCurrentDate(doobieState: DoobieBenchmarkState): LocalDate =
    doobieState.currentDateF.unsafeRunSync()

}
