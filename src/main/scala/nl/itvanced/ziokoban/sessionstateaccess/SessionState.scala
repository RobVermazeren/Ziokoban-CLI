package nl.itvanced.ziokoban.sessionstateaccess

import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.model.LevelId

final case class LevelState(id: LevelId, solved: Boolean) {
  def asSolved: LevelState = this.copy(solved = true)
}

final case class SessionState(
  currentLevelIndex: Int,
  levels: Vector[LevelState]
) {
  val numberOfLevels = levels.size

  /** Return updated `SessionState` where current level is marked as solved. */
  def markSolved(): SessionState = {
    val currentLevelState = levels(currentLevelIndex)
    val newSolvedLevels = levels.updated(currentLevelIndex, currentLevelState.asSolved)
    SessionState(currentLevelIndex, newSolvedLevels)
  }

  /** Return update `SessionState` where current level is moved to next unsolved level. */
  def moveToNextUnsolvedLevel(): SessionState = {
    def firstAfter(i: Int): Option[Int] = ((i+1) until numberOfLevels).find(!levels(_).solved) 
    def firstBefore(i: Int): Option[Int] = (0 to i).find(!levels(_).solved) 

    val newCurrentLevelIndex =  
      firstAfter(currentLevelIndex)
        .orElse(firstBefore(currentLevelIndex))
        .getOrElse(0)

    SessionState(newCurrentLevelIndex, levels)
  }

  /** Return update `SessionState` where current level is moved to next level. */
  def moveToNextLevel(): SessionState = {
    println(s"currentIndex: $currentLevelIndex")
    val newCurrentLevelIndex = {
      val n = currentLevelIndex + 1
      if (n < numberOfLevels) n else 0
    } 
    println(s"newCurrentIndex: $newCurrentLevelIndex")

    SessionState(newCurrentLevelIndex, levels)
  }

  /** Return update `SessionState` where current level is moved to previous level. */
  def moveToPreviousLevel(): SessionState = {
    val newCurrentLevelIndex = {
      val n = currentLevelIndex - 1
      if (n < 0) numberOfLevels - 1 else n
    } 

    SessionState(newCurrentLevelIndex, levels)
  }
}

object SessionState {
  def initial(lc: LevelCollection): SessionState = {
    val cl = 0
    val sl = lc.levels.map(level => LevelState(id = level.id, solved = false))
    SessionState(cl, sl)
  }
}