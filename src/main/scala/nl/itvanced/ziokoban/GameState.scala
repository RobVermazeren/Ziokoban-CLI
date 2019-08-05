package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Model._
import nl.itvanced.ziokoban.Model.Direction._

// Utility case class for holding information of a Location (in a LevelMap).
case class LocationInfo(isValid: Boolean, holdsCrate: Boolean)

case class GameStep(pusherLocation: Coord, crateLocations: Set[Coord], appliedDirection: Direction)

case class GameState private (
    level: Level,
    pusherLocation: Coord,
    crateLocations: Set[Coord],
    isSolved: Option[Boolean] = None,
    history: List[GameStep] = Nil
) {
  private def isValidLocation(location: Coord): Boolean =
    level.spaces.contains(location)

  private def hasCrateAt(location: Coord): Boolean =
    crateLocations.contains(location)

  def locationInfo(location: Coord): LocationInfo =
    LocationInfo(isValidLocation(location), hasCrateAt(location))

  lazy val spacesMap: GameState.SpacesMap = {
    level.spaces.map { c =>
      val isTarget = level.targetLocations contains c
      val space =
        if (pusherLocation == c) Space(Pusher, isTarget)
        else if (crateLocations contains c) Space(Crate, isTarget)
        else Space(Empty, isTarget)

      c -> space
    }.toMap
  }

  def stopped: GameState = this.copy(isSolved = Some(false))
  def isFinished: Boolean = isSolved.isDefined
  def changePusherLocation(newPusherLocation: Coord, d: Direction) = {
    this.copy(pusherLocation = newPusherLocation, history = latestGameStep(d) :: history)
  }
  def changePusherAndCrateLocation(newPusherLocation: Coord, newCrateLocation: Coord, d: Direction) = {
    val newCrateLocations = crateLocations - newPusherLocation + newCrateLocation
    val newIsSolved = if (newCrateLocations == level.targetLocations) Some(true) else None
    this.copy(
      pusherLocation = newPusherLocation,
      crateLocations = newCrateLocations,
      isSolved = newIsSolved,
      history = latestGameStep(d) :: history
    )
  }
  private def latestGameStep(d: Direction) = GameStep(pusherLocation, crateLocations, d)
}

object GameState {

  type SpacesMap = Map[Coord, Space]

  def apply(level: Level): GameState =
    GameState(level, level.pusherLocation, level.crateLocations)
  // Starting from gs, return the GameState that is the result of moving the pusher in direction d.
  def move(gs: GameState, d: Direction): GameState = {
    val newLocation = applyDirection(gs.pusherLocation, d)
    gs.locationInfo(newLocation) match {
      case LocationInfo(false, _) =>
        // Move not possible: new location is outside given gs locations.
        gs
      case LocationInfo(true, false) =>
        // Move in given direction goes to empty location.
        gs.changePusherLocation(newLocation, d)
      case LocationInfo(true, true) =>
        // Move in given direction will hit a crate. Check if crate can be moved.
        val newCrateLocation = applyDirection(newLocation, d)
        gs.locationInfo(newCrateLocation) match {
          case LocationInfo(false, _) =>
            // Move not possible: crate would go outside given gs locations.
            gs
          case LocationInfo(true, true) =>
            // Move not possible: another crate is blocking this move.
            gs
          case LocationInfo(true, false) =>
            // Crate can be moved
            gs.changePusherAndCrateLocation(newLocation, newCrateLocation, d)
        }
    }
  }

  def undo(gs: GameState): GameState = gs.history match {
    case h :: tail => 
      gs.copy(pusherLocation = h.pusherLocation, crateLocations = h.crateLocations, history = tail)
    case Nil => 
      gs // No history to go back to
  }

  def allSteps(gs: GameState): String = gs.history.map(_.appliedDirection).map(direction2char).mkString.reverse

  private def direction2char(d: Direction) = d match {
    case Down =>  'd'
    case Left =>  'l'
    case Right => 'r'
    case Up =>    'u'
  }
  // Return the Coord that results from moving from c into direction d.
  private def applyDirection(c: Coord, d: Direction): Coord = d match {
    case Direction.Up    => Coord(c.x, c.y - 1)
    case Direction.Right => Coord(c.x + 1, c.y)
    case Direction.Down  => Coord(c.x, c.y + 1)
    case Direction.Left  => Coord(c.x - 1, c.y)
  }
}