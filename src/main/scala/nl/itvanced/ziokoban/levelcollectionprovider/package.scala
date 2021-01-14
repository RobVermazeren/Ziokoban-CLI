package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.levels.LevelCollection

package object levelcollectionprovider {

  type LevelCollectionProvider = Has[LevelCollectionProvider.Service]

  object LevelCollectionProvider {

    trait Service {
      def loadLevelCollection(): Task[LevelCollection]
    }

    // Accessor methods
    def loadLevelCollection(): ZIO[LevelCollectionProvider, Throwable, LevelCollection] =
      ZIO.accessM[LevelCollectionProvider](_.get.loadLevelCollection())

  }

}
