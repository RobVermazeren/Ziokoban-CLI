package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.levels.LevelCollection

package object levelsSource {
  
  type LevelsSource = Has[LevelsSource.Service]

  object LevelsSource {
    trait Service {
      def loadLevel(id: String): Task[Option[Level]] // RVNONE: Will be removed, after levelCollections are used.
      def loadLevelCollection(): Task[LevelCollection]  
    }

    // Accessor methods
    def loadLevel(id: String): ZIO[LevelsSource, Throwable, Option[Level]] =
      ZIO.accessM[LevelsSource](_.get.loadLevel(id))
    def loadLevelCollection(): ZIO[LevelsSource, Throwable, LevelCollection] =
      ZIO.accessM[LevelsSource](_.get.loadLevelCollection())  
  }
}
