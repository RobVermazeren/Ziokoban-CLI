package nl.itvanced.ziokoban.levelsProvider

import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.config._
import nl.itvanced.ziokoban.Level
import nl.itvanced.ziokoban.levels.LevelCollection
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.levels.slc.{Example, SLC}
import scala.io.Source
import scala.util.{Failure, Try}
import nl.itvanced.ziokoban.levels.slc.SlcSokobanLevels

object FilesystemLevelsProvider {
  
  val live: ZLayer[Has[Config], FilesystemLevelsProviderError, LevelsProvider] =
    ZLayer.fromEffect(newLiveService())

  def newLiveService(): ZIO[Has[Config], FilesystemLevelsProviderError, LevelsProvider.Service] = 
    for {
      config    <- config[Config]
      directory <- ZIO.fromEither(validatedDirectory(config.directory))
    } yield LiveService(directory, config.current)

  private def validatedDirectory(dir: String): Either[FilesystemLevelsProviderError, os.Path] = {

    def checkExists(path: os.Path): Either[FilesystemLevelsProviderError, os.Path] = 
      if (os.exists(path))
        Right(path)
      else 
        Left(FilesystemLevelsProviderError.LevelsDirectoryUnknown)    

    checkExists(os.pwd / dir)
  }  

  case class LiveService(levelsDirectory: os.Path, currentLevelsFile: Option[String]) extends LevelsProvider.Service {

    final def loadLevelCollection(): Task[LevelCollection] = {
      val t = for {
        fileName    <- Try(currentLevelsFile.get) // RVNOTE: don't like the get.
        fileContent <- Try(os.read(levelsDirectory / s"$fileName.slc"))
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
