package adjacencylist

import adjacencylist.AdjacencyListFirstInsert.getArmy
import cats.effect.kernel.Resource
import tools.TransactorAndSession.*
import cats.effect.{Clock, ExitCode, IO, IOApp, Resource, Sync}
import cats.implicits.*
import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import models.*

import scala.util.Random
import natchez.Trace.Implicits.noop
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import tools.MyTimer.printMeasure

object AdjacencyListInsert extends IOApp {

  private def insertArmyDoobie(units: List[AdjacencyListUnit])(implicit transactor: Transactor[IO]): IO[Unit] =
    IO.blocking {
      val sql =
        "insert into adjacency_list_hierarchy_doobie (id, rank, name, parent_id) values (?, ?, ?, ?)"
      Update[AdjacencyListUnit](sql)
        .updateMany(units)
        .transact(transactor)
        .void
    }.flatten

  private def insertArmySkunk(units: List[AdjacencyListUnit])(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val enc     = (int4 *: varchar *: varchar *: int4.opt).to[AdjacencyListUnit].values.list(units)
      val command =
        sql"insert into adjacency_list_hierarchy_skunk (id, rank, name, parent_id) values $enc".command
      skunkSession.prepare(command).flatMap(ps => ps.execute(units)).void
    }.flatten

  private def insertArmyDoobieChunked(batchSize: Int)(implicit transactor: Transactor[IO]): IO[Unit] =
    IO.blocking {
      getArmy.flatMap { units =>
        units
          .sliding(batchSize, batchSize)
          .toList
          .traverse(batch => insertArmyDoobie(batch))
          .void
      }
    }.flatten

  private def insertArmySkunkChunked(batchSize: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      getArmy.flatMap { units =>
        units
          .sliding(batchSize, batchSize)
          .toList
          .traverse(batch => insertArmySkunk(batch))
          .void
      }
    }.flatten

  private def doobieMobilization(batchSize: Int)(implicit transactor: Transactor[IO]): IO[Unit] =
    printMeasure(insertArmyDoobieChunked(batchSize), "insertArmyDoobie")

  private def skunkMobilization(batchSize: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    printMeasure(insertArmySkunkChunked(batchSize), "insertArmySkunk")

  private def getMaxSkunkBatchSize(nColumns: Int): IO[Int] = IO.delay(32767 / nColumns)

  override def run(args: List[String]): IO[ExitCode] = for {
    batchSize <- getMaxSkunkBatchSize(4)
    _         <- transactorResource.use(implicit transactor => doobieMobilization(batchSize))
    _         <- sessionResource.use(implicit session => skunkMobilization(batchSize))
  } yield ExitCode.Success

}
