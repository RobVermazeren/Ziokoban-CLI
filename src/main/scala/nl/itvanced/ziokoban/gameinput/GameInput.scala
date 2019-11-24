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

  //=== Version of GameInput that will (re)play a given sequence of GameCommands ===
  final case class HardcodedGameInput(
      queue: Queue[GameCommand],
      ref: Ref[Option[GameCommand]]
  ) extends GameInput {
    val gameInput: Service[Any] = new Service[Any] {
      import GameCommands._
      // Return next command, taken from Ref. Ref will contain None afterwards.
      final def nextCommand(): UIO[Option[GameCommand]] =
        for {
          gc <- ref.modify(c => (c, None))
        } yield gc
    }
  }

  object HardcodedGameInput {
    val definedCommands = List(
      MoveUp,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveLeft,
      Quit
    )
    val definedCommands_1 = List(
      MoveUp,
      MoveRight,
      MoveRight,
      MoveRight,
      MoveRight,
      MoveUp,
      MoveRight,
      MoveDown,
      Quit
    )

    // Create a HardcodedGameInput instance with a queue containing the given commands. Empty Ref provided.
    def apply(commands: List[GameCommand]): UIO[GameInput] =
      for {
        q <- Queue.bounded[GameCommand](100)
        _ <- q.offerAll(HardcodedGameInput.definedCommands)
        r <- Ref.make[Option[GameCommand]](None)
        _ <- processQueue(r, q).fork // start process that keeps filling ref with head of queue, until this one is empty.
      } yield new HardcodedGameInput(q, r)

    // Define a process that will fill Ref from Queue until it is empty. Only fill Ref if empty.
    private def processQueue(
        ref: Ref[Option[GameCommand]],
        queue: Queue[GameCommand]
    ): UIO[Unit] = {
      ref.get flatMap {
        case Some(_) =>
          // There is still a command in Ref, waiting to be processed by the game loop.
          UIO.succeed(())

        case None =>
          for {
            // Pass the next value from Queue to Ref.
            newCommand <- queue.poll
            _ <- ref.set(newCommand)
          } yield ()
      }
    }.repeat(Schedule.spaced(Duration(500, TimeUnit.MILLISECONDS)))
      .provide(Clock.Live)
      .unit
  }

  //=== Version of GameInput that takes input from JLine NonBlockingReader ===
  final case class JLineGameInput private (
      queue: Queue[GameCommand]
  ) extends GameInput {
    val gameInput: Service[Any] = new Service[Any] {
      import GameCommands._
      // Return next command, taken from Ref. Ref will contain None afterwards.
      final def nextCommand(): UIO[Option[GameCommand]] =
        for {
          g <- queue.take.fork // Wait for next command on seperate fiber
          gc <- g.join
        } yield Some(gc)
    }
  }

  object JLineGameInput {
    import zio.blocking._

    def apply(): Task[GameInput] =
      for {
        q <- Queue.bounded[GameCommand](100)
        nbr <- ZIO.effect[NonBlockingReader](createReader())
        _ <- handleJLineInput(q, nbr).fork // start process that reads keys from JLine terminal and pushes these as GameCommands to Queue.
      } yield new JLineGameInput(q)

    private def handleJLineInput(
        queue: Queue[GameCommand],
        reader: NonBlockingReader
    ): UIO[Unit] = {
      for {
        e <- ZIO
          .effect[Int](reader.read)
          .mapError(_ => "Read error")
          .either // TODO: bit too simplified error handling.
        gce <- UIO.succeed(e.flatMap { i =>
          i match {
            case 'w' | 65 => Right(GameCommands.MoveUp)
            case 'a' | 68 => Right(GameCommands.MoveLeft)
            case 's' | 66 => Right(GameCommands.MoveDown)
            case 'd' | 67 => Right(GameCommands.MoveRight)
            case 120 => Right(GameCommands.Undo) //  x key
            case 'q' => Right(GameCommands.Quit)
            case _   => Left("Unused key")
          }
        })
        _ <- gce match {
          case Right(gc) => queue.offer(gc)
          case Left(s) => ZIO.unit // TODO: any errors are ignored.
        }
      } yield ()
    }.repeat(Schedule.spaced(Duration(2, TimeUnit.MILLISECONDS)))
      .provide(Clock.Live)
      .unit

    def createReader(): NonBlockingReader = {
      val terminal = org.jline.terminal.TerminalBuilder
        .builder()
        .jna(true)
        .system(true)
        .build()
      terminal.enterRawMode()
      terminal.reader()
    }
  }
}
