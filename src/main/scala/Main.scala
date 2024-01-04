import cats.effect.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import natchez.Trace.Implicits.noop

object Main extends IOApp {

  private val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "frm-comparison-db",
      password = Some("password")
    )

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      for
        d <- s.unique(sql"select current_date".query(date))
        _ <- IO.println(s"The current date is $d.")
      yield ExitCode.Success
    }

}
