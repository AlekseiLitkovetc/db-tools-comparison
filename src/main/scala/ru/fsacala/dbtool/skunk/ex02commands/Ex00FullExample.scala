package ru.fsacala.dbtool.skunk.ex02commands

import cats.Monad
import cats.effect.*
import cats.syntax.all.*
import ru.fsacala.dbtool.skunk.sessionResource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

case class Pet(name: String, age: Short)

trait PetService[F[_]] {
  def insert(pet: Pet): F[Unit]
  def insert(ps: List[Pet]): F[Unit]
  def selectAll: F[List[Pet]]
}

// a companion with a constructor
object PetService {

  // command to insert a pet
  private val insertOne: Command[Pet] =
    sql"INSERT INTO pets VALUES ($varchar, $int2)".command.to[Pet]

  // command to insert a specific list of pets
  private def insertMany(ps: List[Pet]): Command[ps.type] =
    val enc = (varchar *: int2).to[Pet].values.list(ps)
    sql"INSERT INTO pets VALUES $enc".command

  // query to select all pets
  private val all: Query[Void, Pet] =
    sql"SELECT name, age FROM pets".query(varchar *: int2).to[Pet]

  // construct a PetService
  def fromSession[F[_]: Monad](s: Session[F]): PetService[F] =
    new PetService[F]:
      def insert(pet: Pet): F[Unit]      = s.prepare(insertOne).flatMap(_.execute(pet)).void
      def insert(ps: List[Pet]): F[Unit] = s.prepare(insertMany(ps)).flatMap(_.execute(ps)).void
      def selectAll: F[List[Pet]]        = s.execute(all)
}

object Ex00FullExample extends IOApp {
  // a resource that creates and drops a temporary table
  private def withPetsTable(s: Session[IO]): Resource[IO, Unit] =
    val alloc = s.execute(sql"CREATE TEMP TABLE pets (name varchar, age int2)".command).void
    val free  = s.execute(sql"DROP TABLE pets".command).void
    Resource.make(alloc)(_ => free)

  // some sample data
  private val bob     = Pet("Bob", 12)
  private val beagles = List(Pet("John", 2), Pet("George", 3), Pet("Paul", 6), Pet("Ringo", 3))

  // our entry point
  def run(args: List[String]): IO[ExitCode] =
    sessionResource
      .flatTap(withPetsTable)
      .map(PetService.fromSession(_))
      .use { s =>
        for {
          _  <- s.insert(bob)
          _  <- s.insert(beagles)
          ps <- s.selectAll
          _  <- ps.traverse(p => IO.println(p))
        } yield ExitCode.Success
      }
}
