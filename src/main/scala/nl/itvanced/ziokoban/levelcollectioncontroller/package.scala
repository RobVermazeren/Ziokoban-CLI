package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model.LevelCollection

package object levelcollectioncontroller {

  type LevelCollectionController = Has[LevelCollectionController.Service]

  object LevelCollectionController {

    trait Service {
      /** Play on single level */
      def playLevelCollection(): Task[Boolean] // RVNOTE: What is best return type here? 
   }

    // Accessor methods
    def playLevelCollection(): ZIO[LevelCollectionController, Throwable, Boolean] =
      ZIO.accessM[LevelCollectionController](_.get.playLevelCollection())
  } 

}
