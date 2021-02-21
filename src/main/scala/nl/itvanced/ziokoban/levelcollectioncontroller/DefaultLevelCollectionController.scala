package nl.itvanced.ziokoban.levelcollectioncontroller

import zio.{Has, Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.gameplay.GamePlayController
import zio.Ref
import nl.itvanced.ziokoban.model.{LevelCollection, PlayingLevel}
import nl.itvanced.ziokoban.gameplay.PlayLevelResult
import nl.itvanced.ziokoban.gameoutput.GameOutput

object DefaultLevelCollectionController {

  val live: ZLayer[LevelCollectionProvider with GamePlayController with GameOutput, Throwable, LevelCollectionController] =
    ZLayer.fromEffect(newLiveService())
  
  /** Create LevelCollectionController inside a ZIO. */  
  def newLiveService(): ZIO[LevelCollectionProvider with GamePlayController with GameOutput, Throwable, LevelCollectionController.Service] = {
    for {
      lc  <- LevelCollectionProvider.loadLevelCollection()
      gpc <- ZIO.service[GamePlayController.Service]
      go  <- ZIO.service[GameOutput.Service]
      st  <- Ref.make(SessionState.initial(lc)) // Ref for storing session state.
    } yield LiveService(lc, gpc, go, st)
  }

  /** Implementation of the Live service for LevelCollectionController.
   *  @param levelCollection The level collection to be played.
   *  @param gamePlayController Interface to GamePlayController service.
   *  @param gameOutput Interface to GameOutput service.
   *  @param sessionState Ref containing the session state.
   *  @return Implementation of the LevelCollectionController service. 
   */
  final case class LiveService(
    levelCollection: LevelCollection, 
    gamePlayController: GamePlayController.Service,
    gameOutput: GameOutput.Service,
    sessionState: Ref[SessionState]
  ) extends LevelCollectionController.Service {
    // RVNOTE: For the moment only playing first level. Eventually: iterate over all non-solved levels.


    // RVNOTE:
    //  Need a ZIO/Task that will be repeated. ZIO.repeat(Schedule.w)
    def playLevelCollectionNew(): Task[Boolean] = {
      // PlayCurrentLevel.
      // Update session state with result of level.  ## Question: what are the commands allowed?
      //   - Won     => "currentLevel is solved", played++
      //   - Lost    => played++
      //   - Aborted => played++ (is same as lost, just that used left early)
      // Ask user what to do next
      //   - Quit: Stop game
      //   - Replay last level: Play
      //   - Replay next: 
      //   - Replay next unsolved:
      //
      // Keep playing until quit.
            ???
    }


    def playLevelCollection(): Task[Boolean] = { // RVNOTE: To be removed.
      firstPlayingLevel(levelCollection) match { 
        case None     => Task.succeed(false)
        case Some(fl) => playLevel(fl)
      }
    }

    private def playLevel(l: PlayingLevel): Task[Boolean] = {
      for {
        result <- gamePlayController.playLevel(l)
        _      <- result match {
          case PlayLevelResult.Solved(s, t) => gameOutput.println("Congratulations, you won!")
          case PlayLevelResult.NotSolved(c) => gameOutput.println("Better luck next time")
        } 
      } yield // {
        true
    }

    // RVNOTE: This is a temporary method so we can play the first level
    private def firstPlayingLevel(lc: LevelCollection): Option[PlayingLevel] =
      for {
        levelSpec    <- lc.levels.headOption
        playingLevel <- PlayingLevel.fromLevelMap(levelSpec.map).toOption
      } yield playingLevel
  } 
}
