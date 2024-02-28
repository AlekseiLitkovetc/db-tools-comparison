package ltree

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits.*
import natchez.Trace.Implicits.noop
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import tools.TransactorAndSession.sessionResource
import models.LtreeUnitLtree
import skunk.data.LTree

object LtreeGoodExample extends IOApp {

  val sessionResourceWithStrategy: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5432,
      user = "user",
      database = "hierarchy",
      password = Some("pwd"),
      strategy = Strategy.SearchPath //без этого ltree не работает!
    )

  private def insertLtreeUnit(): IO[Unit] =
    sessionResourceWithStrategy.use { skunkSession =>
      val ltreeUnit = LtreeUnitLtree(
        id = 1000001,
        rank = "test",
        name = "test",
        path = skunk.data.LTree.fromString("test").toOption.getOrElse(skunk.data.LTree.Empty)
      )
      val units     = List(ltreeUnit)

      val enc     = (int4 *: varchar *: varchar *: ltree).to[LtreeUnitLtree].values.list(units)
      val command =
        sql"insert into ltree_hierarchy_skunk (id, rank, name, path) values $enc".command
      skunkSession.prepare(command).flatMap(ps => ps.execute(units)).void
    }

  private def selectLtreeUnit(): IO[Unit] =
    sessionResourceWithStrategy.use { skunkSession =>
      val query = sql"select * from ltree_hierarchy_skunk where id = 1000001"
        .query(int4 *: varchar *: varchar *: ltree)
        .to[LtreeUnitLtree]
      val resF  = skunkSession.execute(query)
      resF.map(res => println(s"selected $res"))
    }

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- insertLtreeUnit()
    _ <- selectLtreeUnit()
  } yield ExitCode.Success
}
