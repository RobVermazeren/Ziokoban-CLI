package nl.itvanced.ziokoban.gameinput

import zio.{Queue, Schedule, Task, UIO, ZIO}
import zio.clock.Clock
import zio.duration.Duration
import java.util.concurrent.TimeUnit
import org.jline.utils.NonBlockingReader

import nl.itvanced.ziokoban.GameCommands
import nl.itvanced.ziokoban.GameCommands.GameCommand

//=== Version of GameInput that takes input from JLine NonBlockingReader ===
final case class JLineGameInput private (
    queue: Queue[GameCommand]
) extends GameInput {
  val gameInput: GameInput.Service[Any] = new GameInput.Service[Any] {
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