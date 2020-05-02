package nl.itvanced.ziokoban

import zio.{Has, Task, ZIO}
import nl.itvanced.ziokoban.GameCommands.GameCommand

package object gameinput {

  type GameInput = Has[GameInput.Service]

  object GameInput {
    trait Service {
      // Return the next command from the input. None if no command present.
      def nextCommand(): Task[Option[GameCommand]]
    }

    // Accessor methods
    def nextCommand(): ZIO[GameInput, Throwable, Option[GameCommand]] =
      ZIO.accessM[GameInput](_.get.nextCommand())
  }
}
