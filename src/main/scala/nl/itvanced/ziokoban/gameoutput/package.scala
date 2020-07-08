package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}

package object gameoutput {

  type GameOutput = Has[GameOutput.Service]

  object GameOutput {

    trait Service {
      def drawGameState(state: GameState): Task[Unit]
      def preDrawing(state: GameState): Task[Unit]
      def postDrawing(state: GameState): Task[Unit]
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
