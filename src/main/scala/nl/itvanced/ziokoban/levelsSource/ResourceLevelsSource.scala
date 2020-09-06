package nl.itvanced.ziokoban.levelsSource

import zio.{Task, UIO, ZIO, ZLayer}
import nl.itvanced.ziokoban.Level
import nl.itvanced.ziokoban.levels.LevelCollection
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.levels.slc.{Example, SLC}
import scala.io.Source
import scala.util.{Failure, Try}
import nl.itvanced.ziokoban.levels.slc.SlcSokobanLevels

object ResourceLevelsSource {

  val live: ZLayer[Any, Throwable, LevelsSource] = {
    ZLayer.succeed(
      LiveService()
    )
  }

  case class LiveService() extends LevelsSource.Service {

    final def loadLevel(id: String): Task[Option[Level]] = {
      loadResource(id) match {
        case Failure(_: java.io.FileNotFoundException) => // File not found is valid outcome, being translated to None
          Task.effect(None)

        case t @ _ =>
          Task.fromTry(t).map(toLevel(_))
      }
    }

    final def loadLevelCollection(): Task[LevelCollection] = {
      val t = (for {
        ss <- SLC.loadFromString(Example.original)
        lc <- SlcSokobanLevels.toLevelCollection(ss)
      } yield lc)
      Task.fromTry(t)
    }

    private def loadResource(resourcePath: String): Try[List[String]] =
      Try {
        Source.fromResource(resourcePath).getLines().toList
      }

    private def toLevel(lines: List[String]): Option[Level] =
      AsciiLevelFormat.toLevelMap(lines).flatMap(Level.fromLevelMap(_)).toOption
  }
}
