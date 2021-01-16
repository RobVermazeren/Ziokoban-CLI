package nl.itvanced.ziokoban.levelcollectionprovider

import zio.{Has, Task, UIO, ZIO, ZLayer}
import zio.config._
import nl.itvanced.ziokoban.PlayingLevel
import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.levels.slc.{Example, SLC}
import scala.io.Source
import scala.util.{Failure, Try}
import nl.itvanced.ziokoban.levels.slc.SlcSokobanLevels

object FilesystemLevelCollectionProvider {

  private val CollectionFileExtension = "slc" 
  private val StatsFileExtension      = "zkb" 
  
  val live: ZLayer[Has[Config], FilesystemLevelCollectionProviderError, LevelCollectionProvider] =
    ZLayer.fromEffect(newLiveService())

  def newLiveService(): ZIO[Has[Config], FilesystemLevelCollectionProviderError, LevelCollectionProvider.Service] = 
    for {
      config    <- config[Config]
      directory <- ZIO.fromEither(validatedDirectory(config.directory))
      current   <- ZIO.fromEither(validatedLevelsFile(directory, config.current, CollectionFileExtension))
    } yield LiveService(
              levelsFile = fullPath(directory, current, CollectionFileExtension),
              statsFile  = Some(fullPath(directory, current, StatsFileExtension))
            )

  private def validatedDirectory(dir: String): Either[FilesystemLevelCollectionProviderError, os.Path] = {

    def checkExists(path: os.Path): Either[FilesystemLevelCollectionProviderError, os.Path] = 
      if (os.exists(path))
        Right(path)
      else 
        Left(FilesystemLevelCollectionProviderError.LevelsDirectoryUnknown)    

    checkExists(os.pwd / dir)
  }  

  private def validatedLevelsFile(directory: os.Path, current: Option[String], extension: String): Either[FilesystemLevelCollectionProviderError, String] = {
    current match {
      case None => 
        Left(FilesystemLevelCollectionProviderError.NoFileConfigured)

      case Some(current) =>
        val filePath = fullPath(directory, current, extension) 
        if (os.exists(filePath))
          Right(current)
        else
          Left(FilesystemLevelCollectionProviderError.SlcFileUnknown(filePath))  
    }
  }

  private def fullPath(directory: os.Path, filename: String, extension: String) = directory / s"$filename.$extension"  

  case class LiveService(levelsFile: os.Path, statsFile: Option[os.Path]) extends LevelCollectionProvider.Service {

    final def loadLevelCollection(): Task[LevelCollection] = // RVNOTE: There should be an error type for SLC format errors (with relevant message). 
      Task.fromTry(
        for {
          fileContent <- Try(os.read(levelsFile))
          ss          <- SLC.loadFromString(fileContent)
          lc          <- SlcSokobanLevels.toLevelCollection(ss)
        } yield lc
      )

    final def levelCollectionStatsPath(): Task[Option[os.Path]] = Task.succeed(statsFile)
  }
}
