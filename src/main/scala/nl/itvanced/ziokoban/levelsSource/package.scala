package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.levels.LevelCollection

package object levelsSource {

  type LevelsSource = Has[LevelsSource.Service]

  object LevelsSource {

    trait Service {
      def loadLevelCollection(): Task[LevelCollection]
    }

    // Accessor methods
    def loadLevelCollection(): ZIO[LevelsSource, Throwable, LevelCollection] =
      ZIO.accessM[LevelsSource](_.get.loadLevelCollection())

  }

}
