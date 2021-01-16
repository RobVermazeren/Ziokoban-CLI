package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model.LevelCollection

package object levelcollectionprovider {

  type LevelCollectionProvider = Has[LevelCollectionProvider.Service]

  object LevelCollectionProvider {

    trait Service {
      /** Load the current configured level collection. */
      def loadLevelCollection(): Task[LevelCollection] 

      /** Return the path of the stats file for the current configured level collection. None means no stats are stored. */
      def levelCollectionStatsPath(): Task[Option[os.Path]]
   }

    // Accessor methods
    def loadLevelCollection(): ZIO[LevelCollectionProvider, Throwable, LevelCollection] =
      ZIO.accessM[LevelCollectionProvider](_.get.loadLevelCollection())

  } 

}
