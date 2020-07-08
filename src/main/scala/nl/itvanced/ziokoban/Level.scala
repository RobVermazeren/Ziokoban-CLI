package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Model._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/** Trait representing a Sokoban level. */
trait Level {
  import nl.itvanced.ziokoban.levels.LevelMap.LevelMap

  /** the source LevelMap */
  def map: LevelMap

  /** calculated height of this map */
  lazy val height: Int = map.keySet.map(_.y).max + 1

  /** calculated width of this map */
  lazy val width: Int = map.keySet.map(_.x).max + 1

  /** valid fields on this map */
  def fields: Set[Coord]

  /** the starting location of the pusher */
  def pusher: Coord

  /** walls on this map */
  def walls: Set[Coord]

  /** @param c location.
    *  @return 'true' if location c on this map contains a wall.
    */
  def isWall(c: Coord): Boolean = walls.contains(c)

  /** initial locations of all crates on this map */
  def crates: Set[Coord]

  /** @param c location.
    *  @return 'true' if location c on this map contains a crate.
    */
  def hasCrate(c: Coord): Boolean = crates.contains(c)

  /** targets on this map */
  def targets: Set[Coord]

  /** @param c location.
    *  @return 'true' if location c on this map is a target.
    */
  def isTarget(c: Coord): Boolean = targets.contains(c)
}

object Level {
  import nl.itvanced.ziokoban.levels.LevelMap
  import nl.itvanced.ziokoban.levels.LevelMap._

  /** Create a level for this map.
    *  @param levelMap map of level to be created.
    *  @return Some containing Level if levelMap represents a valid level, None otherwise.
    */
  def fromLevelMap(levelMap: LevelMap): Try[Level] = {
    val fields = levelMap.filter {
      case (_, Field(_, _)) => true
      case _                => false
    }
    val wallLocations = levelMap.filter {
      case (_, Wall) => true
      case _         => false
    }.keySet
    for {
      pusherLocation <- getPusherLocation(fields)
      crateLocations  = getCrateLocations(fields)
      targetLocations = getTargetLocations(fields)
      if (crateLocations.size == targetLocations.size)
      reachableFields = LevelMap.reachableFields(levelMap, pusherLocation)
      if (crateLocations subsetOf reachableFields)
      if (targetLocations subsetOf reachableFields)
    } yield new Level {
      val map: LevelMap       = levelMap
      val fields: Set[Coord]  = reachableFields
      val walls: Set[Coord]   = wallLocations
      val pusher: Coord       = pusherLocation
      val crates: Set[Coord]  = crateLocations
      val targets: Set[Coord] = targetLocations
    }
  }

  /** Return the pusher location.
    *  Only valid if map contains exactly one pusher.
    *  @param map the source LevelMap.
    *  @return The pusher location, if valid.
    */
  private def getPusherLocation(map: LevelMap): Try[Coord] = {
    map
      .filter {
        case (_, tile) => isFieldWithOccupant(tile, Pusher)
      }
      .keySet
      .toList match { // There must be exactly one pusher on a level.
      case h :: Nil => Success(h)
      case _        => Failure(new Exception("Level should contain exactly one pusher."))
    }
  }

  /** Return all crate locations.
    *  @param map the source LevelMap.
    *  @return A Set of all crate locations.
    */
  private def getCrateLocations(map: LevelMap): Set[Coord] = {
    map.filter {
      case (_, tile) => isFieldWithOccupant(tile, Crate)
    }.keySet
  }

  /** Return all target locations.
    *  @param map the source LevelMap.
    *  @return A Set of all target locations.
    */
  private def getTargetLocations(map: LevelMap): Set[Coord] = {
    map.filter {
      case (_, Field(_, true)) => true
      case _                   => false
    }.keySet
  }
}
