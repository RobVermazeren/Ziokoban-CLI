package nl.itvanced.ziokoban.levelcollectioncontroller

import zio.{Has, Ref, Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.gameplay.GamePlayController
import nl.itvanced.ziokoban.model.{GameCommand, LevelCollection, PlayingLevel}
import nl.itvanced.ziokoban.gameplay.PlayLevelResult
import nl.itvanced.ziokoban.gameoutput.GameOutput

object DefaultLevelCollectionController {
  import PlayLevelResult._

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


    // RVNOTE: Add scaladoc
    def playLevelCollection(): Task[Boolean] = {
      playCurrentLevel.repeatUntil {
        case NotSolved(GameCommand.Quit) => true
        case _                           => false
      }.map {
        _ => true // RVNOTE: Still need to decide best return type. Unit?
      } 
    }

    val playCurrentLevel: Task[PlayLevelResult] = {
      for {
        sessionState <- sessionState.get
        currentLevel <- getLevelByIndex(levelCollection, sessionState.currentLevelIndex)
        levelResult  <- gamePlayController.playLevel(currentLevel) 
        _            <- updateSessionState(levelResult)
      } yield levelResult  
    }

    def getLevelByIndex(lc: LevelCollection, index: Int): Task[PlayingLevel] = 
      for {
        levelSpec    <- Task.effect(lc.levels.apply(index)) // levels(i) may throw exception
        playingLevel <- Task.fromTry(PlayingLevel.fromLevelMap(levelSpec.map))
      } yield playingLevel

    def updateSessionState(r: PlayLevelResult): Task[Unit] = 
      r match {
        case Solved(steps, _) => sessionState.update(ss => 
          ss.markSolved().moveToNextUnsolvedLevel()
        ) 

        case NotSolved(command) => command match {
          case GameCommand.Next => 
            sessionState.update(_.moveToNextLevel())

          case GameCommand.NextUnsolved => 
            sessionState.update(_.moveToNextUnsolvedLevel())

          case GameCommand.Previous =>
            sessionState.update(_.moveToPreviousLevel())

          case _                => 
            Task.unit 
        }
      }


    def playLevelCollectionOld(): Task[Boolean] = { // RVNOTE: To be removed.
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
          case PlayLevelResult.NotSolved(c) => gameOutput.println(s"Exit with $c. Better luck next time")
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
