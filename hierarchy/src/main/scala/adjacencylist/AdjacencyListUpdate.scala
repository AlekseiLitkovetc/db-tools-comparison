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

object AdjacencyListUpdate extends IOApp {

  private def withoutORMSkunk(limit: Int)(implicit skunkSession: Session[IO]): IO[Unit] =
    IO.blocking {
      val query =
        sql"""
           insert into ltree_hierarchy_skunk
           with recursive subordinates as (select *, 1 as level, name as sort_path
                                           from adjacency_list_hierarchy
                                           where id = 1
                                           union
                                           select t.*, level + 1, sort_path || '.' || t.name
                                           from adjacency_list_hierarchy t
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
           |insert into ltree_hierarchy_doobie
           |with recursive subordinates as (select *, 1 as level, name as sort_path
           |                                from adjacency_list_hierarchy
           |                                where id = 1
           |                                union
           |                                select t.*, level + 1, sort_path || '.' || t.name
           |                                from adjacency_list_hierarchy t
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
    _ <- transactorResource.use(implicit transactor => printMeasure(withoutORMDoobie(1000000), "withoutORMDoobie"))
    _ <- sessionResource.use(implicit session => printMeasure(withoutORMSkunk(1000000), "withoutORMSkunk"))
  } yield ExitCode.Success

}
