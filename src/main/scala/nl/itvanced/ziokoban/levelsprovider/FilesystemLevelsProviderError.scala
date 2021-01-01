package nl.itvanced.ziokoban.levelsprovider

import nl.itvanced.ziokoban.error.ZiokobanError

sealed trait FilesystemLevelsProviderError extends ZiokobanError
  
object FilesystemLevelsProviderError {
  case object LevelsDirectoryUnknown       extends FilesystemLevelsProviderError {
    val message = "levels.directory does not exist"
  }
  case object NoFileConfigured extends FilesystemLevelsProviderError {
    val message = "levels.current not configured"
  }
  case class SlcFileUnknown(path: os.Path) extends FilesystemLevelsProviderError {
    def message = s"Configured levels.current file does not exist [${path.toString}]"
  }
}
