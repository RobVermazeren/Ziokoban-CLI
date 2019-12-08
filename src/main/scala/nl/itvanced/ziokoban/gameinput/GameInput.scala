package nl.itvanced.ziokoban.gameinput

import java.util.concurrent.TimeUnit

import nl.itvanced.ziokoban.GameCommands
import zio.clock.Clock
import zio.duration.Duration
import zio.{Queue, Ref, Schedule, Task, UIO, ZIO}
import org.jline.utils.NonBlockingReader

trait GameInput extends Serializable {
  def gameInput: GameInput.Service[Any]
}

object GameInput extends Serializable {
  import GameCommands._
  trait Service[R] {
    // Return the next command from the input. None if no command present.
    def nextCommand(): ZIO[R, Nothing, Option[GameCommand]]
  }

  // Helper object, with methods that delegate to the corresponding method from the Environment.
  object > extends GameInput.Service[GameInput] {
    def nextCommand: ZIO[GameInput, Nothing, Option[GameCommands.GameCommand]] =
      ZIO.accessM(_.gameInput nextCommand())
  }
}
