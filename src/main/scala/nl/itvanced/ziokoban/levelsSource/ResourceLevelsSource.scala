package nl.itvanced.ziokoban.levelsSource

import nl.itvanced.ziokoban.{Level, AsciiLevelFormat}
import zio.{Task, UIO}
import scala.io.Source
import scala.util.{Failure, Try}

trait ResourceLevelsSource extends LevelsSource {
  def levelsSource: LevelsSource.Service[Any] = new LevelsSource.Service[Any] {
    final def loadLevel(id: String): Task[Option[Level]] = { 
      loadResource(id) match {
        case Failure(_: java.io.FileNotFoundException) => Task.effect(None) // File not found is valid outcome, being translated to None
        case t@_                               => Task.fromTry(t).map(toLevel(_))
      }
    }
  }

  private def loadResource(resourcePath: String): Try[List[String]] = Try {
    Source.fromResource(resourcePath).getLines.toList
  }

  private def toLevel(lines: List[String]): Option[Level] =
    AsciiLevelFormat.toLevelMap(lines).flatMap(Level.fromLevelMap(_))
}

object ResourceLevelsSource {
  def apply(): UIO[LevelsSource] = UIO(new ResourceLevelsSource {})
}