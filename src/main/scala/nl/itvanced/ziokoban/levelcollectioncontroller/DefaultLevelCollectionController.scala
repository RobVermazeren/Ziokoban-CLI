package nl.itvanced.ziokoban.levelcollectioncontroller

import zio.{Has, Ref, Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.gameplay._
import nl.itvanced.ziokoban.model._
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
        sessionState <- sessionState.get
        currentLevel <- getLevelByIndex(levelCollection, sessionState.currentLevelIndex)
        levelResult  <- gamePlayController.playLevel(currentLevel) 
        _            <- updateSessionState(levelResult)
      } yield levelResult  
    }

    /** Return the level from a `LevelCollection` by index.
     *  @param lc A `LevelCollection`
     *  @param index Index identifying the desired level from this collection.
     *  @return the level at the index-th location in this collection.
     */
    def getLevelByIndex(lc: LevelCollection, index: Int): Task[PlayingLevel] = 
      for {
        levelSpec    <- Task.effect(lc.levels.apply(index)) // levels(i) may throw exception
        playingLevel <- Task.fromTry(PlayingLevel.fromLevelMap(levelSpec.map))
      } yield playingLevel

      /** Update the current `SessionState` based on a `PlayLevelResult`.
        * @param r The result of playing the current level.
        * @return Updated `SessionState`
        */
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
    }
}
