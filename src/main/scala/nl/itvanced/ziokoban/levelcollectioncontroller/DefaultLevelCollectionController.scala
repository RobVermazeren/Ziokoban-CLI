package nl.itvanced.ziokoban.levelcollectioncontroller

import zio.{Has, Ref, Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.gameplay._
import nl.itvanced.ziokoban.model._
import nl.itvanced.ziokoban.gameoutput.GameOutput
import nl.itvanced.ziokoban.sessionstateaccess.SessionStateAccess

object DefaultLevelCollectionController {
  import PlayLevelResult._

  val live: ZLayer[SessionStateAccess with GamePlayController with GameOutput, Throwable, LevelCollectionController] =
    ZLayer.fromEffect(newLiveService())
  
  /** Create LevelCollectionController inside a ZIO. */  
  def newLiveService(): ZIO[SessionStateAccess with GamePlayController with GameOutput, Throwable, LevelCollectionController.Service] = {
    for {
      ssa <- ZIO.service[SessionStateAccess.Service]
      gpc <- ZIO.service[GamePlayController.Service]
      go  <- ZIO.service[GameOutput.Service]
    } yield LiveService(ssa, gpc, go)
  }

  /** Implementation of the Live service for LevelCollectionController.
   *  @param sessionStateAcces Interface to SessionStateAccess service.
   *  @param gamePlayController Interface to GamePlayController service.
   *  @param gameOutput Interface to GameOutput service.
   *  @return Implementation of the LevelCollectionController service. 
   */
  final case class LiveService(
    sessionStateAccess: SessionStateAccess.Service, 
    gamePlayController: GamePlayController.Service,
    gameOutput: GameOutput.Service,
  ) extends LevelCollectionController.Service {


    /** Play the level collection. */
    def playLevelCollection(): Task[Boolean] = {
      playCurrentLevel.repeatUntil {
        case NotSolved(GameCommand.Quit) => true
        case _                           => false
      }.map {
        _ => true // RVNOTE: Still need to decide best return type. Unit?
      } 
    }

    /** Play the current level (in session state) and
     *  update the session state based on the result. 
     */
    val playCurrentLevel: Task[PlayLevelResult] = {
      for {
        currentLevel <- sessionStateAccess.getCurrentLevel()
        levelResult  <- gamePlayController.playLevel(currentLevel) 
        _            <- updateSessionState(levelResult)
      } yield levelResult  
    }

    /** Update the current `SessionState` based on a `PlayLevelResult`.
      * @param r The result of playing the current level.
      * @return Updated `SessionState`
      */
    def updateSessionState(r: PlayLevelResult): Task[Unit] = 
      r match {
        case Solved(steps, _) => 
          for {
            _ <- sessionStateAccess.markSolved(steps)
            _ <- sessionStateAccess.moveToNextUnsolvedLevel()
          } yield ()  

        case NotSolved(command) => command match {
          case GameCommand.Next => 
            sessionStateAccess.moveToNextLevel().unit

          case GameCommand.NextUnsolved => 
            sessionStateAccess.moveToNextUnsolvedLevel().unit

          case GameCommand.Previous =>
            sessionStateAccess.moveToPreviousLevel().unit

          case _ => 
            Task.unit 
        }
      }
    }
}
