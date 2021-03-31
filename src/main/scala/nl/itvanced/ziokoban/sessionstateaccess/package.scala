package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.model.PlayingLevel
import nl.itvanced.ziokoban.model.GameMove

package object sessionstateaccess {

  type SessionStateAccess = Has[SessionStateAccess.Service]

  object SessionStateAccess {

    trait Service {
      /** Return the current level from the configured level collection. */
      def getCurrentLevel(): Task[PlayingLevel] 
      /** Mark current level solved. */
      def markSolved(steps: List[GameMove]): Task[Unit]
      /** Move to next level. */
      def moveToNextLevel(): Task[Int]
      /** Move to next unsolved level. */
      def moveToNextUnsolvedLevel(): Task[Int]
      /** Move to previous level. */
      def moveToPreviousLevel(): Task[Int]
    }

    // Accessor methods
    def getCurrentLevel(): ZIO[SessionStateAccess, Throwable, PlayingLevel] =
      ZIO.accessM[SessionStateAccess](_.get.getCurrentLevel())
    def markSolved(steps: List[GameMove]): ZIO[SessionStateAccess, Throwable, Unit] = 
      ZIO.accessM[SessionStateAccess](_.get.markSolved(steps))  
    def moveToNextLevel(): ZIO[SessionStateAccess, Throwable, Int] = 
      ZIO.accessM[SessionStateAccess](_.get.moveToNextLevel())  
    def moveToNextUnsolvedLevel(): ZIO[SessionStateAccess, Throwable, Int] = 
      ZIO.accessM[SessionStateAccess](_.get.moveToNextUnsolvedLevel())  
    def moveToPreviousLevel(): ZIO[SessionStateAccess, Throwable, Int] = 
      ZIO.accessM[SessionStateAccess](_.get.moveToPreviousLevel())  
  } 
}