package nl.itvanced.ziokoban

import nl.itvanced.ziokoban
import zio.ZIO

package object gameoutput extends GameOutput.Service[GameOutput] {
  final val gameOutputService: ZIO[GameOutput, Nothing, GameOutput.Service[Any]] =
    ZIO.access(_.gameOutput)

  final def drawGameState(state: GameState): ZIO[GameOutput, Throwable, Unit] =
    ZIO.accessM(_.gameOutput drawGameState state)

  final def preDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
    ZIO.accessM(_.gameOutput preDrawing state)  

  final def postDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
    ZIO.accessM(_.gameOutput postDrawing state)  

  final def println[A](text: A): ZIO[GameOutput, Throwable, Unit] =
    ZIO.accessM(_.gameOutput println text)
}
