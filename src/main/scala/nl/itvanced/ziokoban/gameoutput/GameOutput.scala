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
}