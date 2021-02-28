package nl.itvanced.ziokoban.levelcollectioncontroller

import nl.itvanced.ziokoban.model.LevelCollection

final case class SessionState private (
  currentLevelIndex: Int,
  solvedLevels: Vector[Boolean]
) {
  val numberOfLevels = solvedLevels.size

  def markSolved(): SessionState = {
    val newSolvedLevels = solvedLevels.updated(currentLevelIndex, true)
    SessionState(currentLevelIndex, newSolvedLevels)
  }

  def moveToNextUnsolvedLevel(): SessionState = {
    def firstAfter(i: Int): Option[Int] = ((i+1) until numberOfLevels).find(!solvedLevels(_)) 
    def firstBefore(i: Int): Option[Int] = (0 to i).find(!solvedLevels(_)) 

    val newCurrentLevelIndex =  
      firstAfter(currentLevelIndex)
        .orElse(firstBefore(currentLevelIndex))
        .getOrElse(0)

    SessionState(newCurrentLevelIndex, solvedLevels)
  }

  def moveToNextLevel(): SessionState = {
    println(s"currentIndex: $currentLevelIndex")
    val newCurrentLevelIndex = {
      val n = currentLevelIndex + 1
      if (n < numberOfLevels) n else 0
    } 
    println(s"newCurrentIndex: $newCurrentLevelIndex")

    SessionState(newCurrentLevelIndex, solvedLevels)
  }

  def moveToPreviousLevel(): SessionState = {
    val newCurrentLevelIndex = {
      val n = currentLevelIndex - 1
      if (n < 0) numberOfLevels - 1 else n
    } 

    SessionState(newCurrentLevelIndex, solvedLevels)
  }
}

object SessionState {
  def initial(lc: LevelCollection): SessionState = {
    val cl = 0
    val sl = lc.levels.map(_ => false)
    SessionState(cl, sl)
  }
}