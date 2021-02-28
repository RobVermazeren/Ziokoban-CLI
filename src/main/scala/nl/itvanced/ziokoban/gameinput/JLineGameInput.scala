package nl.itvanced.ziokoban.gameinput

import zio.{Queue, Ref, Schedule, Task, UIO, ZIO, ZLayer}
import zio.clock.Clock
import zio.duration.Duration
import java.util.concurrent.TimeUnit
import org.jline.utils.NonBlockingReader

import nl.itvanced.ziokoban.model.GameCommand
import zio.blocking.Blocking

//=== Version of GameInput that takes input from JLine NonBlockingReader ===
object JLineGameInput {
  import GameCommand._

  val live: ZLayer[Clock, Throwable, GameInput] =
    ZLayer.fromEffect(
      newLiveService()
    )

  // Create a JLineGameInput instance with a queue containing the given commands, feeded from JLine NonBlockingReader.
  def newLiveService(): ZIO[Clock, Throwable, GameInput.Service] =
    for {
      q   <- Queue.bounded[GameCommand](100)
      nbr <- ZIO.effect[NonBlockingReader](createReader()) // Can throw exception, so Throwable error type.
      _ <- handleJLineInput(
        q,
        nbr
      ).fork // start process that reads keys from JLine terminal and pushes these as GameCommands to Queue.
    } yield LiveService(q)

  final case class LiveService private (
    queue: Queue[GameCommand]
  ) extends GameInput.Service {

    // Return next command, taken from Ref. Ref will contain None afterwards.
    final def nextCommand(): UIO[Option[GameCommand]] =
      for {
        g  <- queue.take.fork // Wait for next command on seperate fiber // RVNOTE: Doesn't queue wait by itself?
        gc <- g.join
      } yield Some(gc)

  }

  private def handleJLineInput(
    queue: Queue[GameCommand],
    reader: NonBlockingReader
  ): ZIO[Clock, Nothing, Unit] = {
    for {
      e <-
        ZIO
          .effect[Int](reader.read)
          .mapError(_ => "Read error")
          .either // RVNOTE: bit too simplified error handling.
      gce <- UIO.succeed(e.flatMap { i =>
        i match {
          case 'w' | 65  => Right(GameCommand.MoveUp)
          case 'a' | 68  => Right(GameCommand.MoveLeft)
          case 's' | 66  => Right(GameCommand.MoveDown)
          case 'd' | 67  => Right(GameCommand.MoveRight)
          case 'x'       => Right(GameCommand.Undo) //  x key
          case 'r'       => Right(GameCommand.Replay)
          case 'n'       => Right(GameCommand.Next)
          case 'u'       => Right(GameCommand.NextUnsolved) 
          case 'p'       => Right(GameCommand.Previous) 
          case 'q'       => Right(GameCommand.Quit)
          case _         => Left("Unused key")
        }
      })
      _ <- gce match {
        case Right(gc) => queue.offer(gc)
        case Left(s)   => ZIO.unit // RVNOTE: any errors are ignored.
      }
    } yield ()
  }.repeat(Schedule.spaced(Duration(2, TimeUnit.MILLISECONDS))).unit

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
