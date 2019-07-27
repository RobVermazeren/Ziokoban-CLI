package nl.itvanced.ziokoban.levelsSource

import nl.itvanced.ziokoban.Level
import zio.ZIO

trait LevelsSource extends Serializable {
  def levelsSource: LevelsSource.Service[Any]
}

object LevelsSource extends Serializable {
  trait Service[R] {
    def loadLevel(id: String): ZIO[R, Throwable, Option[Level]]
  }
}