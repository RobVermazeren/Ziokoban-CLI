package nl.itvanced.ziokoban

import zio.ZIO

package object levelsSource extends LevelsSource.Service[LevelsSource] {
  final val levelsSourceService: ZIO[LevelsSource, Nothing, LevelsSource.Service[Any]] =
    ZIO.access(_.levelsSource)

  final def loadLevel(id: String): ZIO[LevelsSource, Throwable, Option[Level]] =
    ZIO.accessM(_.levelsSource loadLevel(id))
}