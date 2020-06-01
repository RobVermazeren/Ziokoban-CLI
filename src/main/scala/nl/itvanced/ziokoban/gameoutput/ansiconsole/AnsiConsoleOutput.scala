package nl.itvanced.ziokoban.gameoutput.ansiconsole

import zio.{Task, UIO, ZIO, ZLayer}
import nl.itvanced.ziokoban.gameoutput.GameOutput

object AnsiConsoleOutput {
  
  def live(c: Config): ZLayer[Any, Throwable, GameOutput] = { // RVNOTE: Move to ZOI Config. This will become a val again.
    ZLayer.succeed(
      LiveService(c)
    )
  }

  final case class LiveService private (
    config: Config
  ) extends GameOutput.Service {  
    import nl.itvanced.ziokoban.GameState
    import nl.itvanced.ziokoban.Model.Coord
    import org.fusesource.jansi.{Ansi, AnsiConsole}
    import zio.Task

    private val gameDrawing = GameDrawing(config.chars)

    import ConsoleDrawing._
    /**
      * Draw game state in console
      */
    final def drawGameState(state: GameState): Task[Unit] = {
      val drawing: ConsoleDrawState[Unit] = for {
        _ <- gameDrawing.drawDynamic(toScreenCoord)(state)
      } yield ()
      val result = drawing.run(Ansi.ansi()).value._1
      Task.effect(AnsiConsole.out.println(result))
    }

    def preDrawing(state: GameState): Task[Unit] = {
      val drawing: ConsoleDrawState[Unit] = for {
        _ <- Draw.saveCursorPosition
        _ <- Draw.eraseScreen
        _ <- Draw.hideCursor
        _ <- Draw.setForeGroundColor(Color.Border)
        _ <- Draw.drawBorder(1, 1, state.level.width + 4, state.level.height + 4)
        _ <- gameDrawing.drawStatic(toScreenCoord)(state.level)
      } yield ()
      val result = drawing.run(Ansi.ansi()).value._1
      Task.effect(AnsiConsole.out.println(result))
    }

    def postDrawing(state: GameState): Task[Unit] = {
      val rowBelowGame = state.level.height + 4
      val drawing: ConsoleDrawState[Unit] = for {
        _ <- Draw.showCursor
        _ <- Draw.restoreCursorPosition
        _ <- Draw.cursorToPosition(rowBelowGame, 1)
      } yield ()
      val result = drawing.run(Ansi.ansi()).value._1
      Task.effect(AnsiConsole.out.println(result))
    }

    /**
      * Print a text to the console and move to next line
      */
    final def println[A](text: A): Task[Unit] = {
      Task.effect(AnsiConsole.out.println(text))
    }

    /** Transform level position to screen position */
    private def toScreenCoord(c: Coord) = Coord(c.x + 3, c.y + 3)
  }
}
