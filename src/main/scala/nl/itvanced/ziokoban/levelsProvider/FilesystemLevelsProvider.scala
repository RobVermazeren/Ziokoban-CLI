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
      current   <- ZIO.fromEither(validatedLevelsFile(directory, config.current))
    } yield LiveService(current)

  private def validatedDirectory(dir: String): Either[FilesystemLevelsProviderError, os.Path] = {

    def checkExists(path: os.Path): Either[FilesystemLevelsProviderError, os.Path] = 
      if (os.exists(path))
        Right(path)
      else 
        Left(FilesystemLevelsProviderError.LevelsDirectoryUnknown)    

    checkExists(os.pwd / dir)
  }  

  private def validatedLevelsFile(directory: os.Path, current: Option[String]): Either[FilesystemLevelsProviderError, os.Path] = {
    current match {
      case None => 
        Left(FilesystemLevelsProviderError.NoFileConfigured)

      case Some(current) =>
        val fullPath = directory / s"$current.slc"
        if (os.exists(fullPath))
          Right(fullPath)
        else
          Left(FilesystemLevelsProviderError.SlcFileUnknown(fullPath))  
    }
  }

  case class LiveService(levelsFile: os.Path) extends LevelsProvider.Service {

    final def loadLevelCollection(): Task[LevelCollection] = {
      val t = for {
        fileContent <- Try(os.read(levelsFile))
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
