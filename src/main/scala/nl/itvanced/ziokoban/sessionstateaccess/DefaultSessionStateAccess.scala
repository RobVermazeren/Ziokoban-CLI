package nl.itvanced.ziokoban.sessionstateaccess

import zio.{Has, Ref, Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.levelcollectioncontroller.SessionState
import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.model.PlayingLevel

object DefaultSessionStateAccess {
  
  val live: ZLayer[LevelCollectionProvider, Throwable, SessionStateAccess] =
    ZLayer.fromEffect(newLiveService())

  /** Create SessionStateAccess inside a ZIO. */  
  def newLiveService(): ZIO[LevelCollectionProvider, Throwable, SessionStateAccess.Service] = {
    for {
      lc  <- LevelCollectionProvider.loadLevelCollection()
      st  <- Ref.make(SessionState.initial(lc)) // Ref for storing session state.
    } yield LiveService(lc, st)
  }

  /** Implementation of the Live service for SessionStateAccess.
   *  @param levelCollection The level collection to be played.
   *  @param sessionState Ref containing the session state.
   *  @return Implementation of the SessionStateAccess service. 
   */
  final case class LiveService(
    levelCollection: LevelCollection, 
    sessionState: Ref[SessionState]
  ) extends SessionStateAccess.Service {
      /** Return the configured level collection. */
      def getCurrentLevel(): Task[PlayingLevel] = 
        for {
          state        <- sessionState.get
          levelSpec    <- Task.effect(levelCollection.levels.apply(state.currentLevelIndex)) 
          playingLevel <- Task.fromTry(PlayingLevel.fromLevelMap(levelSpec.map))
        } yield playingLevel

      /** Mark current level solved. */
      def markSolved(): Task[Unit] = 
        sessionState.update(_.markSolved())

      /** Move to next level. */
      def moveToNextLevel(): Task[Int] = 
        sessionState.updateAndGet(_.moveToNextLevel()).map(_.currentLevelIndex)

      /** Move to next unsolved level. */
      def moveToNextUnsolvedLevel(): Task[Int] = 
        sessionState.updateAndGet(_.moveToNextUnsolvedLevel()).map(_.currentLevelIndex)

      /** Move to previous level. */
      def moveToPreviousLevel(): Task[Int] = 
        sessionState.updateAndGet(_.moveToPreviousLevel()).map(_.currentLevelIndex)
  }
}
