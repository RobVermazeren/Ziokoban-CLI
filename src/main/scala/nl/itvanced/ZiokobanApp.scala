package nl.itvanced

import zio.{App, UIO, ZEnv}
import zio.ExitCode

object ZiokobanApp extends App {
  import zio.console.putStrLn
  import nl.itvanced.ziokoban.{GameState, Level}
  import nl.itvanced.ziokoban.Model.Direction
  import nl.itvanced.ziokoban.gameoutput._
  import nl.itvanced.ziokoban.gameinput._
  import nl.itvanced.ziokoban.levelsSource._
  import nl.itvanced.ziokoban.GameCommands._
  import nl.itvanced.ziokoban.config.GameConfig
  import nl.itvanced.ziokoban.gameoutput.ansiconsole.AnsiConsoleOutput
  import zio.ZIO

  /**
    * Implementation of the run method from App.
    *
    * @param args Command line arguments.
    * @return     Final ZIO returning an ExitCode and with all errors applied.
    */
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    program().exitCode
    // RVNOTE: println for error code putStrLn(s"Execution failed with $err") *> 
  }

  def program(): ZIO[ZEnv, Throwable, Unit] = {
    for {
      c <- GameConfig.load() // RVNOTE: when config is a layer this method will be simpler. No for comprehension needed.
      _ <- {
        val gameInputLayer = JLineGameInput.live
        val gameOutputLayer = AnsiConsoleOutput.live(c.gameOutput)
        val levelsSourceLayer = ResourceLevelsSource.live

        val layers = gameInputLayer ++ gameOutputLayer ++ levelsSourceLayer

        makeProgram.provideSomeLayer[ZEnv](layers) // Provide all the required layers, except ZEnv. 
      }
    } yield ()
  }

  val makeProgram: ZIO[GameOutput with GameInput with LevelsSource, Throwable, Unit] = { 
    for {
      l <- LevelsSource.loadLevel("levels/test.sok")
      _ <- l match {
          case None =>        
            GameOutput.println("This is not a valid level")

          case Some(level) => 
            for {
              won <- playLevel(level)
            } yield {
              if (won) GameOutput.println("Congratulations, you won!") 
              else     GameOutput.println("Better luck next time")
            } *> GameOutput.println("Thank you for playing ZIOKOBAN") 
          }
    } yield ()
  }

  private def playLevel(level: Level): ZIO[GameOutput with GameInput, Throwable, Boolean]  = { 
    val gameState = GameState.startLevel(level)
    for {
      _  <- GameOutput.preDrawing(gameState)
      _  <- GameOutput.drawGameState(gameState)
      gs <- gameLoop(gameState)
      _  <- GameOutput.postDrawing(gs)
      _  <- gs.isSolved match {
        case Some(true) => GameOutput.println(s"Congratulations, you won! \n\nYour steps were ${GameState.allStepsString(gs)}\n")
        case _ => GameOutput.println("Better luck next time")
      }
      _  <- GameOutput.println("Thank you for playing ZIOKOBAN")
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
  
  private def processCommand(gc: GameCommand, gs: GameState): ZIO[GameOutput, Throwable, GameState] = gc match {
     case mc: MoveCommand => {
       val newGameState = GameState.move(gs, moveCommand2Direction(mc))
       for {
         _ <- GameOutput.drawGameState(newGameState)
       } yield newGameState
     }
     case Undo => {
       val newGameState = GameState.undo(gs)
       for {
         _ <- GameOutput.drawGameState(newGameState)
       } yield newGameState
     }
     case Quit => ZIO.effectTotal[GameState](GameState.stopGame(gs))
     case Noop =>
       ZIO.effectTotal[GameState](gs) // RVNOTE: wait for a moment?
  }

  private def moveCommand2Direction(mc: MoveCommand): Direction = mc match { 
    case MoveUp    => Direction.Up
    case MoveRight => Direction.Right
    case MoveDown  => Direction.Down
    case MoveLeft  => Direction.Left
  }
}