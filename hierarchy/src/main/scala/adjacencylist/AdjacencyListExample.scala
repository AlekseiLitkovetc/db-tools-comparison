package adjacencylist

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

object AdjacencyListExample extends IOApp {

  private def printMeasure[A](fa: IO[A], name: String): IO[A] =
    for {
      _      <- IO.println(s"started $name")
      start  <- IO.monotonic
      res    <- fa
      finish <- IO.monotonic
      millis  = (finish - start).toMillis
      _      <- IO.println(s"$name:$millis millis")
    } yield res

  private case class RankIdRange(start: Int, end: Int)

  private def getArmy: IO[List[AdjacencyListUnit]] = IO.blocking {

    val defenceMinistersRange   = RankIdRange(0, 1)
    val colonelGeneralsRange    = RankIdRange(1, 11)
    val lieutenantGeneralsRange = RankIdRange(11, 111)
    val colonelsRange           = RankIdRange(111, 1111)
    val lieutenantColonelsRange = RankIdRange(1111, 3111)
    val majorsRange             = RankIdRange(3111, 13111)
    val captainsRange           = RankIdRange(13111, 33111)
    val lieutenantsRange        = RankIdRange(33111, 83111)
    val sergeantsRange          = RankIdRange(83111, 183111)
    val soldiersRange           = RankIdRange(183111, 1000000)

    val defenceMinisters   = List(AdjacencyListUnit(1, "defenseMinister", "defenseMinister", None))
    val colonelGenerals    = buildUnits("colonelGeneral", colonelGeneralsRange, defenceMinistersRange)
    val lieutenantGenerals = buildUnits("lieutenantGeneral", lieutenantGeneralsRange, colonelGeneralsRange)
    val colonels           = buildUnits("colonel", colonelsRange, lieutenantGeneralsRange)
    val lieutenantColonels = buildUnits("lieutenantColonel", lieutenantColonelsRange, colonelsRange)
    val majors             = buildUnits("major", majorsRange, lieutenantColonelsRange)
    val captains           = buildUnits("captain", captainsRange, majorsRange)
    val lieutenants        = buildUnits("lieutenant", lieutenantsRange, captainsRange)
    val sergeants          = buildUnits("sergeant", sergeantsRange, lieutenantsRange)
    val soldiers           = buildUnits("soldier", soldiersRange, sergeantsRange)

    List(
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
  }

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

  private def insertArmyDoobie(units: List[AdjacencyListUnit])(implicit transactor: Transactor[IO]): IO[Unit] =
    IO.blocking {
      val sql =
        "insert into hierarchy.adjacency_list_hierarchy_doobie23 (id, rank, name, parent_id) values (?, ?, ?, ?)"
      Update[AdjacencyListUnit](sql)
        .updateMany(units)
        .transact(transactor)
        .void
    }.flatten

  private def insertArmySkunk(units: List[AdjacencyListUnit])(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val enc     = (int4 *: varchar *: varchar *: int4.opt).to[AdjacencyListUnit].values.list(units)
      val command =
        sql"insert into hierarchy.adjacency_list_hierarchy_skunk23 (id, rank, name, parent_id) values $enc".command
      skunkSession.prepare(command).flatMap(ps => ps.execute(units)).void
    }.flatten

//  private def insertArmySkunkOtherEncoderObject(
//      units: List[AdjacencyListUnit]
//  )(implicit skunkSession: Session[IO]): IO[Unit] =
//    IO.blocking {
//      val unitsOtherObject = units.take(10)
  /* val enc = (int4 *: varchar *: varchar *: int4.opt).to[AdjacencyListUnit].values.list(unitsOtherObject) */
//      val command          =
//        sql"insert into hierarchy.adjacency_list_hierarchy_skunk5 (id, rank, name, parent_id) values $enc".command
//      skunkSession.prepare(command).flatMap(ps => ps.execute(units)).void
//    }.flatten

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

  private def readUnitsSkunk(limit: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val query = sql"select * from hierarchy.adjacency_list_hierarchy_doobie12"
        .query(int4 *: varchar *: varchar *: int4.opt)
        .to[AdjacencyListUnit]
      val resF  = skunkSession.execute(query)
      resF.map(res => println(s"selected ${res.size} units"))
    }.flatten

  private def readUnitsSkunk2(limit: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val query = sql"select * from hierarchy.adjacency_list_hierarchy_doobie23 limit $int4"
        .query(int4 *: varchar *: varchar *: int4.opt)
        .to[AdjacencyListUnit]
      skunkSession
        .prepare(query)
        .flatMap { ps =>
          ps
            .stream(limit, 1024)
            .compile
            .toList
        }
        .map(res => println(s"selected ${res.size} units"))
    }.flatten

  private def selectDoobie(limit: Int)(implicit transactor: Transactor[IO]): IO[Unit] =
    IO.blocking {
      val fr = doobie.Fragment(s"select * from hierarchy.adjacency_list_hierarchy_doobie23 limit $limit", List.empty)
      fr
        .query[AdjacencyListUnit]
        .to[List]
        .transact(transactor)
        .map(res => println(s"selected ${res.size} units"))
    }.flatten

  private def withoutORMSkunk(limit: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val query =
        sql"""
           insert into hierarchy.ltree_hierarchy_skunk5
           with recursive subordinates as (select *, 1 as level, name as sort_path
                                           from hierarchy.adjacency_list_hierarchy
                                           where id = 1
                                           union
                                           select t.*, level + 1, sort_path || '.' || t.name
                                           from hierarchy.adjacency_list_hierarchy t
                                                    inner join subordinates s on s.id = t.parent_id)
           select id, rank, name, sort_path::ltree
           from subordinates
           limit $int4;
           """.command
      skunkSession
        .prepare(query)
        .flatMap(ps => ps.execute(limit))
        .map(code => println(s"executed with $code"))
    }.flatten

  private def withoutORMDoobie(limit: Int)(implicit transactor: Transactor[IO]): IO[Unit] =
    IO.blocking {
      val query =
        s"""
           |insert into hierarchy.ltree_hierarchy_doobie5
           |with recursive subordinates as (select *, 1 as level, name as sort_path
           |                                from hierarchy.adjacency_list_hierarchy
           |                                where id = 1
           |                                union
           |                                select t.*, level + 1, sort_path || '.' || t.name
           |                                from hierarchy.adjacency_list_hierarchy t
           |                                         inner join subordinates s on s.id = t.parent_id)
           |select id, rank, name, sort_path::ltree
           |from subordinates
           |limit $limit;
           |""".stripMargin
      val fr    = doobie.Fragment(query, List.empty)
      fr.update.run
        .transact(transactor)
        .map(code => println(s"executed with $code"))
    }.flatten

  override def run(args: List[String]): IO[ExitCode] = for {
    batchSize <- getMaxSkunkBatchSize(4)
//    _         <- transactorResource.use(implicit transactor => doobieMobilization(batchSize))
//    _         <- sessionResource.use(implicit session => skunkMobilization(batchSize))

//    _ <- transactorResource.use(implicit transactor => printMeasure(selectDoobie(1000000), "select doobie"))
//    _ <- sessionResource.use(implicit session => printMeasure(readUnitsSkunk2(1000000), "select skunk"))

    /* _ <- transactorResource.use(implicit transactor => printMeasure(withoutORMDoobie(1000000), "withoutORMDoobie")) */
//    _ <- sessionResource.use(implicit session => printMeasure(withoutORMSkunk(1000000), "withoutORMSkunk"))
  } yield ExitCode.Success

}
