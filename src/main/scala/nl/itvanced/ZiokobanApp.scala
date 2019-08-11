package nl.itvanced

import zio.App

object ZiokobanApp extends App {
  import nl.itvanced.ziokoban.{GameState, Level}
  import nl.itvanced.ziokoban.Model.Direction
  import nl.itvanced.ziokoban.gameoutput._
  import nl.itvanced.ziokoban.gameinput._
  import nl.itvanced.ziokoban.levelsSource._
  import nl.itvanced.ziokoban.GameCommands._
  import nl.itvanced.ziokoban.config.GameConfig
  import nl.itvanced.ziokoban.gameoutput.ansiconsole.AnsiConsoleOutput
  import zio.ZIO

  def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    for { 
      conf   <- GameConfig.load()
      env    <- createEnvironment(conf)
      result <- startGame.provide(env)
    } yield result
  }.either.map(_.fold(_ => 1, _ => 0))

  def createEnvironment(c: GameConfig): ZIO[Environment, Throwable, GameOutput with GameInput with LevelsSource] = 
    for {
      input <- GameInput.JLineGameInput()
      output <- AnsiConsoleOutput(c.gameOutput)
      source <- ResourceLevelsSource()
    } yield {
      new GameOutput with GameInput with LevelsSource {
        val gameInput = input.gameInput
        val gameOutput = output.gameOutput
        val levelsSource = source.levelsSource
      }
    }

  def startGame: ZIO[GameOutput with GameInput with LevelsSource, Throwable, Unit] = {
    for {
      l <- loadLevel("levels/test.sok")
      _ <- l match {
          case None =>        
            println("This is not a valid level")

          case Some(level) => 
            for {
              won <- playLevel(level)
            } yield {
              if (won) println("Congratulations, you won!") 
              else     println("Better luck next time")
            } *> println("Thank you for playing ZIOKOBAN") 
          }
    } yield ()
  }

  private def playLevel(level: Level): ZIO[GameOutput with GameInput, Throwable, Boolean]  = { 
    val gameState = GameState.newForLevel(level)
    for {
      _  <- preDrawing(gameState)
      _  <- drawGameState(gameState)
      gs <- gameLoop(gameState)
      _  <- postDrawing(gs)
      _  <- gs.isSolved match {
        case Some(true) => println(s"Congratulations, you won! \n\nYour steps were ${GameState.allSteps(gs)}\n")
        case _ => println("Better luck next time")
      }
      _  <- println("Thank you for playing ZIOKOBAN")
    } yield gs.isSolved.getOrElse(false)
  }

  private def gameLoop(
      gs: GameState
  ): ZIO[GameOutput with GameInput, Throwable, GameState] =
    for {
      c <- nextCommand()
      r <- {
        c.getOrElse(Noop) match {
          case mc: MoveCommand => {
            val newGameState = GameState.move(gs, moveCommand2Direction(mc))
            for {
              _ <- drawGameState(newGameState)
            } yield newGameState
          }
          case Undo => {
            val newGameState = GameState.undo(gs)
            for {
              _ <- drawGameState(newGameState)
            } yield newGameState
          }
          case Quit => ZIO.effectTotal[GameState](GameState.stopGame(gs))
          case Noop =>
            ZIO.effectTotal[GameState](gs) // RVNOTE: wait for a moment?
        }
      }
      s <- if (r.isFinished) ZIO.succeed(r) else gameLoop(r)
    } yield s

  private def moveCommand2Direction(mc: MoveCommand): Direction = mc match { 
    case MoveUp    => Direction.Up
    case MoveRight => Direction.Right
    case MoveDown  => Direction.Down
    case MoveLeft  => Direction.Left
  }
}