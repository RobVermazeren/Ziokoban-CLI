package nl.itvanced.ziokoban.levelcollectioncontroller

import nl.itvanced.ziokoban.model.LevelCollection

final case class SessionState private (
  currentLevelIndex: Int,
  solvedLevels: Vector[Boolean]
) {
  val numberOfLevels = solvedLevels.size

  def updated(levelIndex: Int, solved: Boolean): SessionState = {
    val newSolvedLevels = solvedLevels.updated(levelIndex, solved)

    def firstAfter(i: Int): Option[Int] = (i until numberOfLevels).find(!solvedLevels(_)) 
    def firstBefore(i: Int): Option[Int] = (0 until i).find(!solvedLevels(_)) 

    val newCurrentLevelIndex =  
      firstAfter(levelIndex)
        .orElse(firstBefore(levelIndex))
        .getOrElse(0)
    
    SessionState(newCurrentLevelIndex, newSolvedLevels)
  }
}

object SessionState {
  def initial(lc: LevelCollection): SessionState = {
    val cl = 0
    val sl = lc.levels.map(_ => false)
    SessionState(cl, sl)
  }
}