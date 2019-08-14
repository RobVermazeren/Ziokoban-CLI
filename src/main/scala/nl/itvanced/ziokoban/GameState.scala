package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Model._
import nl.itvanced.ziokoban.Model.Direction._

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
}

object GameState {

  def startLevel(sourceLevel: Level) = new GameState {
     val level = sourceLevel
     val pusher = level.pusher
     val crates = level.crates
     val isSolved = None
     val history = Nil
  }

  def stopGame(gs: GameState) = new GameState {
    val level = gs.level
    val pusher = gs.pusher
    val crates = gs.crates
    val isSolved = Some(false) // Ends this game
    val history = gs.history
  } 

  def changePusherLocation(gs: GameState, newPusherLocation: Coord, d: Direction) = new GameState {
    val level = gs.level
    val pusher = newPusherLocation
    val crates = gs.crates
    val isSolved = None
    val history = GameStep(gs.pusher, gs.crates, d) :: gs.history
  }
   
  def changePusherAndCrateLocation(gs: GameState, newPusherLocation: Coord, newCrateLocation: Coord, d: Direction): GameState  = {
    val newCrateLocations = gs.crates - newPusherLocation + newCrateLocation
    val newIsSolved = if (newCrateLocations == gs.level.targets) Some(true) else None

   new GameState {
    val level = gs.level
    val pusher = newPusherLocation
    val crates = newCrateLocations
    val isSolved = newIsSolved
    val history = GameStep(gs.pusher, gs.crates, d) :: gs.history
   }
  }

  // Starting from gs, return the GameState that is the result of moving the pusher in direction d.
  def move(gs: GameState, d: Direction): GameState = {
    def isValidLocation(c: Coord): Boolean = gs.level.fields.contains(c)
    def hasCrate(c: Coord): Boolean = gs.crates.contains(c)

    val newPusherLocation = applyDirection(gs.pusher, d)
    if (isValidLocation(newPusherLocation)) {
      if (!hasCrate(newPusherLocation)) 
        // Move in given direction goes to empty location.
        changePusherLocation(gs, newPusherLocation, d)
      else {
        // Move in given direction will hit a crate. Check if crate can be moved.
        val newCrateLocation = applyDirection(newPusherLocation, d)
        if (isValidLocation(newCrateLocation)) {
          if (hasCrate(newCrateLocation)) 
            // Move not possible: another crate is blocking this move.
            gs
          else 
            // Crate can be moved
            changePusherAndCrateLocation(gs, newPusherLocation, newCrateLocation, d) 
        }
        else 
          // Move not possible: crate would go outside given gs locations.
          gs
      }
    }
    else 
      // Move not possible: new location is outside given gs locations.
      gs
  }

  /** Undo the last game step.
   *  @param gs the current game state.
   *  @return the new game state with the undo applied.
   */
  def undo(gs: GameState): GameState = gs.history match {
    case h :: tail => 
      new GameState {
        val level = gs.level
        val pusher = h.pusherLocation
        val crates = h.crateLocations
        val isSolved = None
        val history = tail 
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
  private def direction2char(d: Direction) = d match {
    case Down  => 'd'
    case Left  => 'l'
    case Right => 'r'
    case Up    => 'u'
  }

  /** Return the Coord that results from moving from c into direction d.
   *  @param c current position.
   *  @param d direction to apply.
   *  @return the new position.
   */
  private def applyDirection(c: Coord, d: Direction): Coord = d match {
    case Direction.Up    => Coord(c.x, c.y - 1)
    case Direction.Right => Coord(c.x + 1, c.y)
    case Direction.Down  => Coord(c.x, c.y + 1)
    case Direction.Left  => Coord(c.x - 1, c.y)
  }
}