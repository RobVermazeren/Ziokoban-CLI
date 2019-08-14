package nl.itvanced.ziokoban.gameoutput.ansiconsole

import cats.data.State
import nl.itvanced.ziokoban.GameState
import nl.itvanced.ziokoban.Model._

final case class GameDrawing(config: CharConfig) {
  import ConsoleDrawing._
  import org.fusesource.jansi.Ansi.Attribute

  private val tileColor = Color(BaseColor.Green, Color.IsBright)
  private val targetTileColor = Color(BaseColor.Red, Color.IsBright)
  private val wallColor = Color(BaseColor.Yellow, !Color.IsBright)

  private def charFor(o: Occupant): Char = o match {
    case Pusher => config.pusherChar 
    case Crate  => config.crateChar 
    case Empty  => ' '
  }

  private def colorFor(o: Occupant): Color = o match {
    case Pusher => Color(BaseColor.Black, !Color.IsBright)
    case Crate => Color(BaseColor.Magenta, !Color.IsBright)
    case Empty => Color(BaseColor.White, Color.IsBright)
  }

  private def drawGameField(c: Coord, o: Occupant, isTarget: Boolean): ConsoleDrawState[Unit] =  
    for {
      _ <- Draw.setForeGroundColor(colorFor(o))
      _ <- Draw.setBackGroundColor(if (isTarget) targetTileColor else tileColor)
      s <- Draw.drawChar(c, charFor(o)) 
    } yield s

  def drawStatic(levelToScreenPosition: Coord => Coord)(gs: GameState): ConsoleDrawState[Unit] = 
    for {
      _ <- Draw.setBackGroundColor(wallColor)
      s <- Draw.drawChars(gs.level.walls.map(levelToScreenPosition), ' ')
    } yield s

  def drawDynamic(levelToScreenPosition: Coord => Coord)(gs: GameState): ConsoleDrawState[Unit] = {
    gs.level.fields.foldLeft(State.pure(Unit): ConsoleDrawState[Unit]) { case (state, coord) =>
      val occupant = if (gs.pusher == coord) Pusher
      else if (gs.crates.contains(coord)) Crate
      else Empty
      state flatMap (_ => drawGameField(levelToScreenPosition(coord), occupant, gs.level.isTarget(coord)))
    }
  }
}
