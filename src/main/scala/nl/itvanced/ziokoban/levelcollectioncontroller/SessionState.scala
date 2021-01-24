package nl.itvanced.ziokoban.levelcollectioncontroller

import nl.itvanced.ziokoban.model.LevelCollection

final case class SessionState private (
  currentLevel: Int,
  solvedLevels: Vector[Boolean]
) {
  val numberOfLevels = solvedLevels.size
}

object SessionState {
  def initial(lc: LevelCollection): SessionState = {
    val cl = 0
    val sl = lc.levels.map(_ => false)
    SessionState(cl, sl)
  }
}