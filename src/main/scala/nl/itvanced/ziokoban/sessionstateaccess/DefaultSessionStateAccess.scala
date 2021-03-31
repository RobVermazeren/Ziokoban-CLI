package nl.itvanced.ziokoban.sessionstateaccess

import zio.{Has, Queue, Ref, Schedule, Task, UIO, ZIO, ZLayer}
import nl.itvanced.ziokoban.levelcollectionprovider.LevelCollectionProvider
import nl.itvanced.ziokoban.model._
import zio.clock.Clock

object DefaultSessionStateAccess {
  
  val live: ZLayer[LevelCollectionProvider with Clock, Throwable, SessionStateAccess] =
    ZLayer.fromEffect(newLiveService())

  /** Create SessionStateAccess inside a ZIO. */  
  def newLiveService(): ZIO[LevelCollectionProvider with Clock, Throwable, SessionStateAccess.Service] = {
    for {
      lc  <- LevelCollectionProvider.loadLevelCollection()
      sp  <- LevelCollectionProvider.levelCollectionStatsPath()
      ss  <- initialSessionState(sp, lc)
      st  <- Ref.make(ss) // Ref for storing session state.
      tq  <- Queue.sliding[SessionState](1) // Trigger queue for persisting session state.
      _   <- persistSessionState(tq, sp).repeat(Schedule.forever).forkDaemon
    } yield new LiveService(lc, st, tq)
  }

  /* 
     Process reads queue in separate fiber (and will thus be suspended until non empty), read the state and will safe to file.
  */
  private def persistSessionState(tq: Queue[SessionState], sp: Option[os.Path]): UIO[Unit] = 
    sp.fold(ZIO.unit)(path =>
      for {
        ssf <- tq.take.fork // Wait for value in queue in separate fiber.
        ss  <- ssf.join 
        _   <- SessionStateStorage.writeToFile(path, ss).ignore
      } yield ()
    )

  /*
    Load session state from file. Initial state for level collection if no file exists.
   */
  private def initialSessionState(sp: Option[os.Path], lc: LevelCollection): Task[SessionState] = 
    sp.fold[Task[SessionState]](newSessionStateForLevelCollection(lc))(path => 
      SessionStateStorage.readFromFile(path).orElse(newSessionStateForLevelCollection(lc))
    )  

  private def newSessionStateForLevelCollection(lc: LevelCollection): Task[SessionState] =
    Task.succeed(SessionState.initial(lc))  

  /** Implementation of the Live service for SessionStateAccess.
   *  @param levelCollection The level collection to be played.
   *  @param sessionState Ref containing the session state.
   *  @param triggerQueue Queue for triggering persisting of the current session state.
   *  @return Implementation of the SessionStateAccess service. 
   */
  final class LiveService(
    levelCollection: LevelCollection, 
    sessionState: Ref[SessionState],
    triggerQueue: Queue[SessionState],
  ) extends SessionStateAccess.Service {
      /** Return the current level from the  configured level collection. */
      def getCurrentLevel(): Task[PlayingLevel] = 
        for {
          state        <- sessionState.get
          levelSpec    <- Task.effect(levelCollection.levels.apply(state.currentLevelIndex)) 
          playingLevel <- Task.fromTry(PlayingLevel.fromLevelMap(levelSpec.map))
        } yield playingLevel

      /** Mark current level solved. */
      def markSolved(steps: List[GameMove]): Task[Unit] = 
        for {
          ss <- sessionState.updateAndGet(_.markSolved(steps))
          _  <- triggerQueue.offer(ss)
        } yield ()

      /** Move to next level. */
      def moveToNextLevel(): Task[Int] = 
        for {
          ss <- sessionState.updateAndGet(_.moveToNextLevel())
          _  <- triggerQueue.offer(ss)
        } yield ss.currentLevelIndex

      /** Move to next unsolved level. */
      def moveToNextUnsolvedLevel(): Task[Int] = 
        for {
          ss <- sessionState.updateAndGet(_.moveToNextUnsolvedLevel())
          _  <- triggerQueue.offer(ss)
        } yield ss.currentLevelIndex

      /** Move to previous level. */
      def moveToPreviousLevel(): Task[Int] = 
        for {
          ss <- sessionState.updateAndGet(_.moveToPreviousLevel())
          _  <- triggerQueue.offer(ss)
        } yield ss.currentLevelIndex
  }
}
