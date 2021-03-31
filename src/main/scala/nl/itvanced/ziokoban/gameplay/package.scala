package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model._

package object gameplay {

  type GamePlayController = Has[GamePlayController.Service]

  sealed trait PlayLevelResult
  object PlayLevelResult {
    /**
      * Level has been solved.
      *
      * @param steps Steps that solved the level. 
      * @param timeInSeconds Time taken (in seconds) to solve the level.
      */
    case class Solved(steps: List[GameMove], timeInSeconds: Int)  extends PlayLevelResult
    /**
      * Level has not been solved. 
      *
      * @param command Command that aborted this attempt to solve level.
      */
    case class NotSolved(command: GameCommand)  extends PlayLevelResult
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
