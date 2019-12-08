package nl.itvanced.ziokoban.gameoutput

import nl.itvanced.ziokoban.GameState
import zio.ZIO

trait GameOutput extends Serializable {
  def gameOutput: GameOutput.Service[Any]
}

object GameOutput extends Serializable {
  trait Service[R] {
    def drawGameState(state: GameState): ZIO[R, Throwable, Unit]
    def preDrawing(state: GameState): ZIO[R, Throwable, Unit]
    def postDrawing(state: GameState): ZIO[R, Throwable, Unit]
    def println[A](text: A): ZIO[R, Throwable, Unit]
  }

  // Helper object, with methods that delegate to the corresponding method from the Environment.
  object > extends GameOutput.Service[GameOutput] {
    final def drawGameState(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM(_.gameOutput drawGameState state)

    final def preDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM(_.gameOutput preDrawing state)  

    final def postDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM(_.gameOutput postDrawing state)  

    final def println[A](text: A): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM(_.gameOutput println text)
  }
}