package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.levels.LevelCollection

package object levelcollectionprovider {

  type LevelsProvider = Has[LevelsProvider.Service]

  object LevelsProvider {

    trait Service {
      def loadLevelCollection(): Task[LevelCollection]
    }

    // Accessor methods
    def loadLevelCollection(): ZIO[LevelsProvider, Throwable, LevelCollection] =
      ZIO.accessM[LevelsProvider](_.get.loadLevelCollection())

  }

}
