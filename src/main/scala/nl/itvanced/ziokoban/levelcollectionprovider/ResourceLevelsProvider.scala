package nl.itvanced.ziokoban.levelcollectionprovider

import zio.{Task, UIO, ZIO, ZLayer}
import nl.itvanced.ziokoban.PlayingLevel
import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.levels.slc.{Example, SLC}
import scala.io.Source
import scala.util.{Failure, Try}
import nl.itvanced.ziokoban.levels.slc.SlcSokobanLevels

object ResourceLevelCollectionProvider {

  val live: ZLayer[Any, Throwable, LevelCollectionProvider] =
    ZLayer.succeed(
      LiveService()
    )

  case class LiveService() extends LevelCollectionProvider.Service {

    final def loadLevelCollection(): Task[LevelCollection] = {
      val t = for {
        ss <- SLC.loadFromString(Example.original)
        lc <- SlcSokobanLevels.toLevelCollection(ss)
      } yield lc
      Task.fromTry(t)
    }

    final def levelCollectionStatsPath(): Task[Option[os.Path]] =
      Task(None)

    private def loadResource(resourcePath: String): Try[List[String]] =
      Try {
        Source.fromResource(resourcePath).getLines().toList
      }

    private def toLevel(lines: List[String]): Option[PlayingLevel] =
      AsciiLevelFormat.toLevelMap(lines).flatMap(PlayingLevel.fromLevelMap(_)).toOption

  }

}
