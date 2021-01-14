package nl.itvanced.ziokoban.levelcollectionprovider

import nl.itvanced.ziokoban.error.ZiokobanError

sealed trait FilesystemLevelCollectionProviderError extends ZiokobanError
  
object FilesystemLevelCollectionProviderError {
  case object LevelsDirectoryUnknown       extends FilesystemLevelCollectionProviderError {
    val message = "levels.directory does not exist"
  }
  case object NoFileConfigured extends FilesystemLevelCollectionProviderError {
    val message = "levels.current not configured"
  }
  case class SlcFileUnknown(path: os.Path) extends FilesystemLevelCollectionProviderError {
    def message = s"Configured levels.current file does not exist [${path.toString}]"
  }
}
