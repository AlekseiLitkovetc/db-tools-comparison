package ltree

import cats.effect.{ExitCode, IO, IOApp}
import tools.TransactorAndSession.transactorResource
import cats.implicits.*
import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import models.LtreeUnitString

object LtreeWrongExample extends IOApp {

  private def insertLtreeUnit(): IO[Unit] =
    transactorResource.use { transactor =>
      val ltreeUnit = LtreeUnitString(id = 1000001, rank = "test", name = "test", path = "test")
      sql"insert into ltree_hierarchy_doobie (id, rank, name, path) values (${ltreeUnit.id}, ${ltreeUnit.rank}, ${ltreeUnit.name}, ${ltreeUnit.path})".update.run
        .transact(transactor)
        .void
    }

  private def selectLtreeUnit(): IO[Unit] =
    transactorResource.use { transactor =>
      val sql = sql"select * from ltree_hierarchy_doobie where id = 1000001"
      sql.query[LtreeUnitString].to[List].transact(transactor).map(println)
    }

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- insertLtreeUnit()
    _ <- selectLtreeUnit()
  } yield ExitCode.Success
}
