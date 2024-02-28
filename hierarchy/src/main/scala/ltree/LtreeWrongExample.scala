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

  private def selectLtreeUnit(): IO[Unit] = transactorResource.use { transactor =>
    val sql = sql"select * from hierarchy.ltree_hierarchy where id = 1000001"
    sql.query[LtreeUnitString].to[List].transact(transactor).map(println)
  }

  private def insertLtreeUnit(u: LtreeUnitString): IO[Unit] = transactorResource.use { transactor =>
    sql"insert into hierarchy.ltree_hierarchy (id, rank, name, path) values (${u.id}, ${u.rank}, ${u.name}, ${u.path})".update.run
      .transact(transactor)
      .void
  }

  private val ltreeUnit = LtreeUnitString(id = 1000001, rank = "test", name = "test", path = "test")

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- insertLtreeUnit(ltreeUnit)
    _ <- selectLtreeUnit()
  } yield ExitCode.Success
}
