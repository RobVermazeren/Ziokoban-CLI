package nl.itvanced.ziokoban.levelsProvider

import nl.itvanced.ziokoban.error.ZiokobanError

sealed trait FilesystemLevelsProviderError extends ZiokobanError
  
object FilesystemLevelsProviderError {
  case object LevelsDirectoryUnknown       extends FilesystemLevelsProviderError {
    val message = "levels.directory does not exist"
  }
}
