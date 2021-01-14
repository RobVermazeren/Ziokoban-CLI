package nl.itvanced

import zio.{App, UIO, ZEnv}
import zio.ExitCode

object ZiokobanApp extends App {
  import zio.console.putStrLn
  import nl.itvanced.ziokoban.{GameState, PlayingLevel}
  import nl.itvanced.ziokoban.model.Direction
  import nl.itvanced.ziokoban.gameoutput._
  import nl.itvanced.ziokoban.gameinput._
  import nl.itvanced.ziokoban.levelsprovider._
  import nl.itvanced.ziokoban.GameCommands._
  import nl.itvanced.ziokoban.config.GameConfig
  import nl.itvanced.ziokoban.gameoutput.ansiconsole.AnsiConsoleOutput
  import zio.ZIO
  import zio.config.syntax._

  /**
   * Implementation of the run method from App.
   *
   * @param args Command line arguments.
   * @return     Final ZIO returning an ExitCode and with all errors applied.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program()
      .catchSome{
        case e: FilesystemLevelsProviderError => putStrLn(s"Cannot start Ziokoban: ${e.message}")
      }
      .tapError(e => ZIO.succeed(e.printStackTrace()))
      .exitCode

  def program(): ZIO[ZEnv, Throwable, Unit] = {
    val config              = GameConfig.asLayer
    val gameInputLayer      = JLineGameInput.live
    val gameOutputLayer     = config.narrow(_.gameOutput) >>> AnsiConsoleOutput.live
    val levelsProviderLayer = config.narrow(_.levels) >>> FilesystemLevelsProvider.live

    val layers = gameInputLayer ++ gameOutputLayer ++ levelsProviderLayer

    makeProgram.provideSomeLayer[ZEnv](layers) // Provide all the required layers, except ZEnv.
  }

  val makeProgram: ZIO[GameOutput with GameInput with LevelsProvider, Throwable, Unit] = {
    for {
      l <- LevelsProvider.loadLevelCollection().map(_.levels.headOption)
      _ <- l match {
        case None =>
          GameOutput.println("This is not a valid level")

        case Some(level) =>
          for {
            won <- playLevel(level)
          } yield {
            if (won) GameOutput.println("Congratulations, you won!")
            else GameOutput.println("Better luck next time")
          } *> GameOutput.println("Thank you for playing ZIOKOBAN")
      }
    } yield ()
  }

  private def playLevel(level: PlayingLevel): ZIO[GameOutput with GameInput, Throwable, Boolean] = {
    val gameState = GameState.startLevel(level)
    for {
      _  <- GameOutput.preDrawing(gameState)
      _  <- GameOutput.drawGameState(gameState)
      gs <- gameLoop(gameState)
      _  <- GameOutput.postDrawing(gs)
      _ <- gs.isSolved match {
        case Some(true) =>
          GameOutput.println(s"Congratulations, you won! \n\nYour steps were ${GameState.allStepsString(gs)}\n")
        case _ => GameOutput.println("Better luck next time")
      }
      _ <- GameOutput.println("Thank you for playing ZIOKOBAN")
    } yield gs.isSolved.getOrElse(false)
  }

  private def gameLoop(
    gs: GameState
  ): ZIO[GameOutput with GameInput, Throwable, GameState] =
    for {
      c <- GameInput.nextCommand()
      r <- processCommand(c.getOrElse(Noop), gs)
      s <- if (r.isFinished) ZIO.succeed(r) else gameLoop(r)
    } yield s

  private def processCommand(gc: GameCommand, gs: GameState): ZIO[GameOutput, Throwable, GameState] =
    gc match {
      case mc: MoveCommand =>
        val newGameState = GameState.move(gs, moveCommand2Direction(mc))
        for {
          _ <- GameOutput.drawGameState(newGameState)
        } yield newGameState
      case Undo =>
        val newGameState = GameState.undo(gs)
        for {
          _ <- GameOutput.drawGameState(newGameState)
        } yield newGameState
      case Quit => ZIO.effectTotal[GameState](GameState.stopGame(gs))
      case Noop =>
        ZIO.effectTotal[GameState](gs) // RVNOTE: wait for a moment?
    }

  private def moveCommand2Direction(mc: MoveCommand): Direction =
    mc match {
      case MoveUp    => Direction.Up
      case MoveRight => Direction.Right
      case MoveDown  => Direction.Down
      case MoveLeft  => Direction.Left
    }

}
