package nl.itvanced.ziokoban.levelsSource

import nl.itvanced.ziokoban.Level
import nl.itvanced.ziokoban.levels.AsciiLevelFormat
import zio.{Task, UIO}
import scala.io.Source
import scala.util.{Failure, Try}

trait ResourceLevelsSource extends LevelsSource { // RVNOTE: this file will be obsolete once slc files are supported. 
  def levelsSource: LevelsSource.Service[Any] = new LevelsSource.Service[Any] {
    final def loadLevel(id: String): Task[Option[Level]] = { 
      loadResource(id) match {
        case Failure(_: java.io.FileNotFoundException) => // File not found is valid outcome, being translated to None
          Task.effect(None) 

        case t@_ => 
          Task.fromTry(t).map(toLevel(_))
      }
    }
  }

  private def loadResource(resourcePath: String): Try[List[String]] = Try {
    Source.fromResource(resourcePath).getLines.toList
  }

  private def toLevel(lines: List[String]): Option[Level] =
    AsciiLevelFormat.toLevelMap(lines).flatMap(Level.fromLevelMap(_)).toOption
}

object ResourceLevelsSource {
  def apply(): UIO[LevelsSource] = UIO(new ResourceLevelsSource {})
}