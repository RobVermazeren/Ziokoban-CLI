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

  private def drawGameSpace(c: Coord, space: Space): ConsoleDrawState[Unit] =  
    for {
      _ <- Draw.setForeGroundColor(colorFor(space.occupant))
      _ <- Draw.setBackGroundColor(if (space.isTarget) targetTileColor else tileColor)
      s <- Draw.drawChar(c, charFor(space.occupant)) 
    } yield s

  def drawStatic(position: Coord => Coord)(gs: GameState): ConsoleDrawState[Unit] = 
    for {
      _ <- Draw.setBackGroundColor(wallColor)
      s <- Draw.drawChars(gs.level.walls.map(position), ' ')
    } yield s

  def drawDynamic(position: Coord => Coord)(gs: GameState): ConsoleDrawState[Unit] = {
    gs.spacesMap.foldLeft(State.pure(Unit): ConsoleDrawState[Unit]) { case (state, (coord, space)) =>
      state flatMap (_ => drawGameSpace(position(coord), space))
    }
  }
}
