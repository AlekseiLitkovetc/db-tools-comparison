package ltree

import cats.effect.{ExitCode, IO, IOApp}
import tools.TransactorAndSession.transactorResource
import cats.implicits.*
import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import models.*

object LtreeExample extends IOApp {

  private def selectLtreeUnit(): IO[Unit] = transactorResource.use { transactor =>
    val sql = sql"select * from hierarchy.ltree_hierarchy where id = 1"
    sql.query[LtreeUnit].to[List].transact(transactor).map(println)
  }

  private def insertLtreeUnit(u: LtreeUnit): IO[Unit] = transactorResource.use { transactor =>
    sql"insert into hierarchy.ltree_hierarchy (id, rank, name, path) values (${u.id}, ${u.rank}, ${u.name}, ${u.path})".update.run
      .transact(transactor)
      .void
  }

  private val ltreeUnit = LtreeUnit(id = 1000001, rank = "test", name = "test", path = "test")

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- selectLtreeUnit()
    _ <- insertLtreeUnit(ltreeUnit)
  } yield ExitCode.Success
}
