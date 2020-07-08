package nl.itvanced.ziokoban.gameoutput.ansiconsole

import cats.data.State
import nl.itvanced.ziokoban.GameState
import nl.itvanced.ziokoban.Level
import nl.itvanced.ziokoban.Model._

/** Class for drawing a game in an ansi console. */
final case class GameDrawing(config: CharConfig) {
  import ConsoleDrawing._
  import org.fusesource.jansi.Ansi.Attribute

  private val tileColor       = Color(BaseColor.Green, Color.IsBright)
  private val targetTileColor = Color(BaseColor.Red, Color.IsBright)
  private val wallColor       = Color(BaseColor.Yellow, !Color.IsBright)

  /** The char used to represent an Occupant.
    *  @param o An occupant.
    *  @return The char for given occupant.
    */
  private def charFor(o: Occupant): Char =
    o match {
      case Pusher => config.pusherChar
      case Crate  => config.crateChar
      case Empty  => ' '
    }

  /** The color used for an Occupant.
    *  @param o An occupant.
    *  @return The color for given occupant.
    */
  private def colorFor(o: Occupant): Color =
    o match {
      case Pusher => Color(BaseColor.Black, !Color.IsBright)
      case Crate  => Color(BaseColor.Magenta, !Color.IsBright)
      case Empty  => Color(BaseColor.White, Color.IsBright)
    }

  /** Return a ConsoleDrawState for drawing a game field.
    *  @param c The coordinate of the desired game field.
    *  @param o The occupant of given game field.
    *  @param isTarget Is given game field a target.
    *  @return The ConsoleDrawState that will draw given game field.
    */
  private def drawGameField(c: Coord, o: Occupant, isTarget: Boolean): ConsoleDrawState[Unit] =
    for {
      _ <- Draw.setForeGroundColor(colorFor(o))
      _ <- Draw.setBackGroundColor(if (isTarget) targetTileColor else tileColor)
      s <- Draw.drawChar(c, charFor(o))
    } yield s

  /** Return a ConsoleDrawState for drawing the static parts of a Level.
    *  @param levelToScreenPosition Function to transform level coordinates to screen coordinates.
    *  @param level The level to draw
    *  @return The ConsoleDrawState that will draw the static parts of given level.
    */
  def drawStatic(levelToScreenPosition: Coord => Coord)(level: Level): ConsoleDrawState[Unit] =
    for {
      _ <- Draw.setBackGroundColor(wallColor)
      s <- Draw.drawChars(level.walls.map(levelToScreenPosition), ' ')
    } yield s

  /** Return a ConsoleDrawState for drawing the dynamic parts of the game state of a level.
    *  @param levelToScreenPosition Function to transform level coordinates to screen coordinates.
    *  @param level The level to draw
    *  @return The ConsoleDrawState that will draw the dynamic parts of given game state of a level.
    */
  def drawDynamic(levelToScreenPosition: Coord => Coord)(gs: GameState): ConsoleDrawState[Unit] = {
    gs.level.fields.foldLeft(State.pure(Unit): ConsoleDrawState[Unit]) {
      case (state, coord) =>
        val occupant =
          if (gs.pusher == coord) Pusher
          else if (gs.crates.contains(coord)) Crate
          else Empty
        state flatMap (_ => drawGameField(levelToScreenPosition(coord), occupant, gs.level.isTarget(coord)))
    }
  }
}
