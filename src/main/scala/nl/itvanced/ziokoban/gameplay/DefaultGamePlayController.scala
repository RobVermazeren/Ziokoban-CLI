package nl.itvanced.ziokoban.gameplay

import zio.{Task, ZIO, ZLayer}
import nl.itvanced.ziokoban.{GameCommands, GameState, PlayingLevel}
import nl.itvanced.ziokoban.gameinput.GameInput
import nl.itvanced.ziokoban.gameoutput.GameOutput
import nl.itvanced.ziokoban.model.Direction

object DefaultGamePlayController {

  val live: ZLayer[GameInput with GameOutput, Nothing, GamePlayController] =
    ZLayer.fromServices[GameInput.Service, GameOutput.Service, GamePlayController.Service](
      (gameInput, gameOutput) =>
        newLiveService(gameInput, gameOutput)
    )

  def newLiveService(gameInput: GameInput.Service, gameOutput: GameOutput.Service): GamePlayController.Service = 
    new GamePlayController.Service {
      import GameCommands._

      def playLevel(level: PlayingLevel): Task[PlayLevelResult] = {
        val gameState = GameState.startLevel(level)
        for {
          _  <- gameOutput.preDrawing(gameState)
          _  <- gameOutput.drawGameState(gameState)
          gs <- gameLoop(gameState)
          _  <- gameOutput.postDrawing(gs)
          _  <- gs.isSolved match {
            case Some(true) =>
              gameOutput.println(s"\nYour steps were ${GameState.allStepsString(gs)}\n") // RVNOTE: Should be moved elsewhere.
            case _ => Task.unit
          }
        } yield 
          gs.isSolved match {
            case Some(true)  => PlayLevelResult.Solved
            case Some(false) => PlayLevelResult.Failed
            case None        => PlayLevelResult.Aborted 
          }
      } 

      private def gameLoop(
        gs: GameState
      ): Task[GameState] =
        for {
          c <- gameInput.nextCommand()
          r <- processCommand(c.getOrElse(Noop), gs)
          s <- if (r.isFinished) Task.succeed(r) else gameLoop(r)
        } yield s

      private def processCommand(gc: GameCommand, gs: GameState): Task[GameState] =
        gc match {
          case mc: MoveCommand =>
            val newGameState = GameState.move(gs, moveCommand2Direction(mc))
            for {
              _ <- gameOutput.drawGameState(newGameState)
            } yield newGameState
          case Undo =>
            val newGameState = GameState.undo(gs)
            for {
              _ <- gameOutput.drawGameState(newGameState)
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
}