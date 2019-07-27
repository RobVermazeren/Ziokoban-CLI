package nl.itvanced.ziokoban

import zio.ZIO

package object gameinput extends GameInput.Service[GameInput] {
  final val gameInputService: ZIO[GameInput, Nothing, GameInput.Service[Any]] =
    ZIO.access(_.gameInput)

  final val nextCommand: ZIO[GameInput, Nothing, Option[GameCommands.GameCommand]] =
    ZIO.accessM(_.gameInput nextCommand())
}
