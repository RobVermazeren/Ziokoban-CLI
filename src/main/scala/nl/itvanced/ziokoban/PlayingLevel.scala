package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Model._
import nl.itvanced.ziokoban.levels.{LevelFieldMap, LevelMap}
import scala.util.Try

/** Trait representing a Sokoban level. */
trait PlayingLevel {

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

  /**
   * @param c location.
   *  @return 'true' if location c on this map contains a wall.
   */
  def isWall(c: Coord): Boolean = walls.contains(c)

  /** initial locations of all crates on this map */
  def crates: Set[Coord]

  /**
   * @param c location.
   *  @return 'true' if location c on this map contains a crate.
   */
  def hasCrate(c: Coord): Boolean = crates.contains(c)

  /** targets on this map */
  def targets: Set[Coord]

  /**
   * @param c location.
   *  @return 'true' if location c on this map is a target.
   */
  def isTarget(c: Coord): Boolean = targets.contains(c)
}

object PlayingLevel {

  /**
   * Create a level for this map.
   *  @param levelMap map of level to be created.
   *  @return Some containing Level if levelMap represents a valid level, None otherwise.
   */
  def fromLevelMap(levelMap: LevelMap): Try[PlayingLevel] = {
    val fieldMap: LevelFieldMap = levelMap.collect {
      case (c, f @ Field(_, _)) => c -> f
    }
    val wallLocations = levelMap.filter {
      case (_, Wall) => true
      case _         => false
    }.keySet
    for {
      pusherLocation <- LevelMap.getPusherLocation(fieldMap)
      crateLocations  = LevelMap.getCrateLocations(fieldMap)
      targetLocations = LevelMap.getTargetLocations(fieldMap)
      if (crateLocations.size == targetLocations.size)
      reachableFields = LevelMap.reachableFields(fieldMap, pusherLocation)
      if (crateLocations subsetOf reachableFields)
      if (targetLocations subsetOf reachableFields)
    } yield new PlayingLevel {
      val map: LevelMap       = levelMap
      val fields: Set[Coord]  = reachableFields
      val walls: Set[Coord]   = wallLocations
      val pusher: Coord       = pusherLocation
      val crates: Set[Coord]  = crateLocations
      val targets: Set[Coord] = targetLocations
    }
  }

}
