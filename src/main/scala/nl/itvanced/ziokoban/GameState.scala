package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Model._
import nl.itvanced.ziokoban.Model.Direction._
import scala.annotation.tailrec

case class GameStep(pusherLocation: Coord, crateLocations: Set[Coord], appliedDirection: Direction)

/** Trait representing the state of a Sokoban game */
trait GameState {

  /** the level being played */
  def level: Level

  /** the location of the pusher */
  def pusher: Coord

  /** the locations of the crates */
  def crates: Set[Coord]

  /** is this level solved?
    *  @return None, if game has not ended.
    *          Some(false), if game ended without the level being solved.
    *          Some(true), if game ended and the level was solved.
    */
  def isSolved: Option[Boolean]

  /** history of the game up untill now */
  def history: List[GameStep]

  /** has this level ended? */
  def isFinished: Boolean = isSolved.isDefined

  /** the empty locations */
  def emptyTiles: Set[Coord] = level.fields -- crates - pusher
}

object GameState {

  /**
    * Return a initial game state for a level.
    *
    * @param sourceLevel Level to use.
    * @return            An initial game state for sourceLevel.
    */
  def startLevel(sourceLevel: Level) =
    new GameState {
      val level    = sourceLevel
      val pusher   = level.pusher
      val crates   = level.crates
      val isSolved = None
      val history  = Nil
    }

  /**
    * Stop a game.
    *
    * @param gs Current game state.
    * @return   Updated game state, representing the stopped game.
    */
  def stopGame(gs: GameState) =
    new GameState {
      val level    = gs.level
      val pusher   = gs.pusher
      val crates   = gs.crates
      val isSolved = Some(false) // Ends this game
      val history  = gs.history
    }

  /**
    * Apply a push to game state gs.
    * Pusher pushes agains 0 or more crates.
    *
    * @param gs                     Current game state.
    * @param newPusherLocation      New location of the pusher.
    * @param emptyLocationToPushTo  The empty location that being pushed to.
    * @param d                      The direction of the push.
    * @return                       Updatde game state.
    */
  def applyPush(gs: GameState, newPusherLocation: Coord, emptyLocationToPushTo: Coord, d: Direction): GameState = {
    val newCrateLocations =
      if (newPusherLocation != emptyLocationToPushTo)
        gs.crates - newPusherLocation + emptyLocationToPushTo
      else gs.crates
    val newIsSolved = if (newCrateLocations == gs.level.targets) Some(true) else None

    new GameState {
      val level    = gs.level
      val pusher   = newPusherLocation
      val crates   = newCrateLocations
      val isSolved = newIsSolved
      val history  = GameStep(gs.pusher, gs.crates, d) :: gs.history
    }
  }

  /**
    * Apply a pusher move to the current game state.
    *
    * @param gs Current game state.
    * @param d  The direction the pusher will move to.
    * @return   Updated game state.
    */
  def move(gs: GameState, d: Direction): GameState = {
    def isValidLocation(c: Coord): Boolean = gs.level.fields.contains(c)
    def hasCrate(c: Coord): Boolean        = gs.crates.contains(c)
    def isEmpty(c: Coord): Boolean         = isValidLocation(c) && !hasCrate(c)

    val MaxPushableCrates = 1 // RVNATE: Move this somewhere else?
    val newPusherLocation = applyDirection(gs.pusher, d)

    searchEmptyTile(gs.crates, gs.emptyTiles, d)(gs.pusher, MaxPushableCrates) match {
      case Some(emptyTile) => applyPush(gs, newPusherLocation, emptyTile, d)
      case None            => gs
    }
  }

  /**
    * Search for an empty tile to perform a move to.
    *
    * @param crates         The current set of crates.
    * @param emptyTiles     The current set of empty tiles.
    * @param d              The direction of the search.
    * @param startLocation  Start location of the search.
    * @param numberOfCrates Number of crates that may be pushed.
    * @return The first empty tile, starting from startLocation, working in given direction, that can be used for moving numberOfCrates tiles.
    */
  @tailrec
  def searchEmptyTile(crates: Set[Coord], emptyTiles: Set[Coord], d: Direction)(
      startLocation: Coord,
      numberOfCrates: Int
  ): Option[Coord] = {
    // numberOfCrates = 0, represents a move where no carts are pushed.
    if (numberOfCrates < 0) None
    else {
      val newLocation = applyDirection(startLocation, d)
      if (emptyTiles.contains(newLocation))
        Some(newLocation)
      else if (!crates.contains(newLocation))
        None
      else
        searchEmptyTile(crates, emptyTiles, d)(newLocation, numberOfCrates - 1)
    }

  }

  /** Undo the last game step.
    *  @param gs the current game state.
    *  @return the new game state with the undo applied.
    */
  def undo(gs: GameState): GameState =
    gs.history match {
      case h :: tail =>
        new GameState {
          val level    = gs.level
          val pusher   = h.pusherLocation
          val crates   = h.crateLocations
          val isSolved = None
          val history  = tail
        }
      case Nil =>
        gs // No history to go back to
    }

  /** Return all game steps.
    *  @param gs the current game state.
    *  @return a string containing all game steps from first to last.
    */
  def allStepsString(gs: GameState): String = gs.history.map(_.appliedDirection).map(direction2char).mkString.reverse

  /** Translate a Direction to a Char */
  private def direction2char(d: Direction) =
    d match {
      case Down  => 'd'
      case Left  => 'l'
      case Right => 'r'
      case Up    => 'u'
    }

  /** Return the Coord that results from moving from c into direction d and taking s steps.
    *  @param c current position.
    *  @param d direction to apply.
    *  @param s number of steps to apply. Defaults to 1.
    *  @return the new position.
    */
  private def applyDirection(c: Coord, d: Direction, s: Int = 1): Coord =
    d match {
      case Direction.Up    => Coord(c.x, c.y - s)
      case Direction.Right => Coord(c.x + s, c.y)
      case Direction.Down  => Coord(c.x, c.y + s)
      case Direction.Left  => Coord(c.x - s, c.y)
    }
}
