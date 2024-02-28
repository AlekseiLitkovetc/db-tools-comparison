package adjacencylist

import cats.effect.kernel.Resource
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import models.*
import natchez.Trace.Implicits.noop
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import tools.MyTimer.printMeasure
import tools.TransactorAndSession.*

import scala.util.Random

object AdjacencyListSelect extends IOApp {

  private def selectSkunk(limit: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val query = sql"select * from hierarchy.adjacency_list_hierarchy limit $int4"
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
      val fr = doobie.Fragment(s"select * from hierarchy.adjacency_list_hierarchy limit $limit", List.empty)
      fr
        .query[AdjacencyListUnit]
        .to[List]
        .transact(transactor)
        .map(res => println(s"selected ${res.size} units"))
    }.flatten

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- transactorResource.use(implicit transactor => printMeasure(selectDoobie(1000000), "select doobie"))
    _ <- sessionResource.use(implicit session => printMeasure(selectSkunk(1000000), "select skunk"))
  } yield ExitCode.Success

}
