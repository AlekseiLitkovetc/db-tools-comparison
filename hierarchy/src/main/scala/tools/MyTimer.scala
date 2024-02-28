package tools

import cats.effect.IO

object MyTimer {

  def printMeasure[A](fa: IO[A], name: String): IO[A] =
    for {
      _      <- IO.println(s"started $name")
      start  <- IO.monotonic
      res    <- fa
      finish <- IO.monotonic
      millis  = (finish - start).toMillis
      _      <- IO.println(s"$name:$millis millis")
    } yield res

}
