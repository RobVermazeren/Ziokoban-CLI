package nl.itvanced.ziokoban.levelsProvider

import zio.{Has, Task, UIO, ZIO, ZLayer}
import nl.itvanced.ziokoban.Level
import nl.itvanced.ziokoban.levels.LevelCollection
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.levels.slc.{Example, SLC}
import scala.io.Source
import scala.util.{Failure, Try}
import nl.itvanced.ziokoban.levels.slc.SlcSokobanLevels

object FilesystemLevelsProvider {
  
  val live: ZLayer[Has[Config], Throwable, LevelsProvider] =
    ZLayer.fromService(config => new LiveService(config))

  case class LiveService(config: Config) extends LevelsProvider.Service {

    val levelsDir = os.pwd / config.directory

    final def loadLevelCollection(): Task[LevelCollection] = {
      val t = for {
        fileName    <- Try(config.current.get) // RVNOTE: don't like the get.
        fileContent <- Try(os.read(levelsDir / s"$fileName.slc"))
        ss          <- SLC.loadFromString(fileContent)
        lc          <- SlcSokobanLevels.toLevelCollection(ss)
      } yield lc
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
