package nl.itvanced.ziokoban.gameinput

import zio.{Queue, Ref, Schedule, Task, UIO, ZIO, ZLayer}
import zio.clock.Clock
import zio.duration.Duration
import java.util.concurrent.TimeUnit

import nl.itvanced.ziokoban.model.GameCommands
import nl.itvanced.ziokoban.model.GameCommands.GameCommand

//=== Version of GameInput that will (re)play a given sequence of GameCommands ===
object HardcodedGameInput {
  import GameCommands._

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

  val live: ZLayer[Clock, Nothing, GameInput] =
    ZLayer.fromEffect(
      newLiveService(definedCommands)
    )

  // Create a HardcodedGameInput instance with a queue containing the given commands. Empty Ref provided.
  def newLiveService(commands: List[GameCommand]): ZIO[Clock, Nothing, GameInput.Service] =
    for {
      q <- Queue.bounded[GameCommand](100)
      _ <- q.offerAll(HardcodedGameInput.definedCommands)
      r <- Ref.make[Option[GameCommand]](None)
      _ <- processQueue(r, q).fork // start process that keeps filling ref with head of queue, until this one is empty.
    } yield new LiveService(q, r)

  final case class LiveService(
    queue: Queue[GameCommand],
    ref: Ref[Option[GameCommand]]
  ) extends GameInput.Service {

    // Return next command, taken from Ref. Ref will contain None afterwards.
    final def nextCommand(): UIO[Option[GameCommand]] =
      for {
        gc <- ref.modify(c => (c, None))
      } yield gc

  }

  // Define a process that will fill Ref from Queue until it is empty. Only fill Ref if empty.
  private def processQueue(
    ref: Ref[Option[GameCommand]],
    queue: Queue[GameCommand]
  ): ZIO[Clock, Nothing, Unit] = {
    ref.get flatMap {
      case Some(_) =>
        // There is still a command in Ref, waiting to be processed by the game loop.
        UIO.succeed(())

      case None =>
        for {
          // Pass the next value from Queue to Ref.
          newCommand <- queue.poll
          _          <- ref.set(newCommand)
        } yield ()
    }
  }.repeat(Schedule.spaced(Duration(500, TimeUnit.MILLISECONDS))).unit

}
