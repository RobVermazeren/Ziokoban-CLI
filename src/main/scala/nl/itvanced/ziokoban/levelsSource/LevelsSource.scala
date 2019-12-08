package nl.itvanced.ziokoban.levelsSource

import nl.itvanced.ziokoban.Level
import zio.ZIO

trait LevelsSource extends Serializable {
  def levelsSource: LevelsSource.Service[Any]
}

object LevelsSource extends Serializable {
  trait Service[R] {
    def loadLevel(id: String): ZIO[R, Throwable, Option[Level]] // Will be removed
//    def loadLevelCollection(): ZIO[R, Throwable, LevelCollection]  
  }
  
  // Helper object, with methods that delegate to the corresponding method from the Environment.
  object > extends LevelsSource.Service[LevelsSource] {
    def loadLevel(id: String) = {
      ZIO.accessM(_.levelsSource loadLevel(id))

    }
  }
}