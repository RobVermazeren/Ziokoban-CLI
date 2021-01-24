package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.model.GameState

package object gameoutput {

  type GameOutput = Has[GameOutput.Service]

  object GameOutput {

    trait Service {
      /** Draw game state. */
      def drawGameState(state: GameState): Task[Unit]
      /** Any drawing before drawing game state. */
      def preDrawing(state: GameState): Task[Unit]
      /** Any drawing after drawing game state. */
      def postDrawing(state: GameState): Task[Unit]
      /** Print a text. */
      def println[A](text: A): Task[Unit]
    }

    // Accessor methods
    def drawGameState(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM[GameOutput](_.get.drawGameState(state))

    def preDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM[GameOutput](_.get.preDrawing(state))

    def postDrawing(state: GameState): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM[GameOutput](_.get.postDrawing(state))

    def println[A](text: A): ZIO[GameOutput, Throwable, Unit] =
      ZIO.accessM[GameOutput](_.get.println(text))

  }

}
