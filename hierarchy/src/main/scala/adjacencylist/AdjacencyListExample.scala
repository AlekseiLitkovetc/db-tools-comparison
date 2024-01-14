package adjacencylist

import tools.TransactorAndSession.*
import cats.effect.{Clock, ExitCode, IO, IOApp, Resource}
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

object AdjacencyListExample extends IOApp {

  private def printMeasure[A](fa: IO[A], name: String): IO[A] =
    for {
      _      <- IO.println(s"started $name")
      start  <- IO.monotonic
      res    <- fa
      finish <- IO.monotonic
      millis  = (finish - start).toMillis
      _       = println(s"$name:$millis millis")
    } yield res

  private case class RankIdRange(start: Int, end: Int)

  private val defenceMinistersRange   = RankIdRange(0, 1)
  private val colonelGeneralsRange    = RankIdRange(1, 11)
  private val lieutenantGeneralsRange = RankIdRange(11, 111)
  private val colonelsRange           = RankIdRange(111, 1111)
  private val lieutenantColonelsRange = RankIdRange(1111, 3111)
  private val majorsRange             = RankIdRange(3111, 13111)
  private val captainsRange           = RankIdRange(13111, 33111)
  private val lieutenantsRange        = RankIdRange(33111, 83111)
  private val sergeantsRange          = RankIdRange(83111, 183111)
  private val soldiersRange           = RankIdRange(183111, 1000000)

  //TODO enum
  private val defenceMinisters   = List(AdjacencyListUnit(1, "defenseMinister", "defenseMinister", None))
  private val colonelGenerals    = buildUnits("colonelGeneral", colonelGeneralsRange, defenceMinistersRange)
  private val lieutenantGenerals = buildUnits("lieutenantGeneral", lieutenantGeneralsRange, colonelGeneralsRange)
  private val colonels           = buildUnits("colonel", colonelsRange, lieutenantGeneralsRange)
  private val lieutenantColonels = buildUnits("lieutenantColonel", lieutenantColonelsRange, colonelsRange)
  private val majors             = buildUnits("major", majorsRange, lieutenantColonelsRange)
  private val captains           = buildUnits("captain", captainsRange, majorsRange)
  private val lieutenants        = buildUnits("lieutenant", lieutenantsRange, captainsRange)
  private val sergeants          = buildUnits("sergeant", sergeantsRange, lieutenantsRange)
  private val soldiers           = buildUnits("soldier", soldiersRange, sergeantsRange)

  private val army = List(
    defenceMinisters,
    colonelGenerals,
    lieutenantGenerals,
    colonels,
    lieutenantColonels,
    majors,
    captains,
    lieutenants,
    sergeants,
    soldiers
  ).flatten

  private def buildUnits(
      rank: String,
      range: RankIdRange,
      parentRange: RankIdRange
  ): List[AdjacencyListUnit] =
    List.range(range.start + 1, range.end + 1).map { id =>
      AdjacencyListUnit(
        id = id,
        rank = rank,
        name = s"${rank}_${id - range.start}",
        parentId = Random.between(parentRange.start + 1, parentRange.end + 1).some
      )
    }

  private def insertArmyDoobie(): IO[Unit] = transactorResource.use { transactor =>
    val sql = "insert into hierarchy.adjacency_list_hierarchy_doobie (id, rank, name, parent_id) values (?, ?, ?, ?)"
    Update[AdjacencyListUnit](sql)
      .updateMany(army)
      .transact(transactor)
      .void
  }

  /** skunk.exception.TooManyParametersException:
    * 🔥
    * 🔥 TooManyParametersException
    * 🔥
    * 🔥 Problem: Statement has more than 32767 parameters.
    * 🔥 Hint: Postgres can't handle this many parameters. Execute multiple statements instead.
    * 🔥
    */
  private def insertArmySkunk(units: List[AdjacencyListUnit]): IO[Unit] = sessionResource.use { skunkSession =>
    val enc     = (int4 *: varchar *: varchar *: int4.opt).to[AdjacencyListUnit].values.list(units)
    val command =
      sql"insert into hierarchy.adjacency_list_hierarchy_skunk (id, rank, name, parent_id) values $enc".command
    skunkSession.prepare(command).flatMap(ps => ps.execute(units)).void
  }

  //  (8191*4)<=32676
  private def insertArmySkunkChunked(): IO[Unit] =
    army.sliding(8191, 8191).toList.traverse(ch => insertArmySkunk(ch)).void

  private def doobieMobilization: IO[Unit] = printMeasure(insertArmyDoobie(), "insertArmyDoobie")

  private def skunkMobilization: IO[Unit] = printMeasure(insertArmySkunkChunked(), "insertArmySkunk")

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- doobieMobilization
    _ <- skunkMobilization
  } yield ExitCode.Success

}
