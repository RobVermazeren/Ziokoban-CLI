package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model.LevelCollection

package object gameplay {

  type GamePlayController = Has[GamePlayController.Service]

  sealed trait PlayLevelResult
  object PlayLevelResult {
    case object Solved  extends PlayLevelResult
    case object Failed  extends PlayLevelResult
    case object Aborted extends PlayLevelResult
  }

  object GamePlayController {

    trait Service {
      /** Play on single level */
      def playLevel(level: PlayingLevel): Task[PlayLevelResult] 
   }

    // Accessor methods
    def playLevel(level: PlayingLevel): ZIO[GamePlayController, Throwable, PlayLevelResult] =
      ZIO.accessM[GamePlayController](_.get.playLevel(level))
  } 

}
