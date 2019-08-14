package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Level.LevelMap
import nl.itvanced.ziokoban.Model._

/** Trait representing a Sokoban level. */ 
trait Level {
  /** the source LevelMap */
  def map: Level.LevelMap

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
  /** Type alias for a map that represents a Level. */
  type LevelMap = Map[Coord, Tile]

  /** Create a level for this map.
   *  @param levelMap map of level to be created.
   *  @return Some containing Level if levelMap represents a valid level, None otherwise.
   */
  def fromLevelMap(levelMap: LevelMap): Option[Level] = { 
    val fields = levelMap.filter {
      case (_, Field(_, _)) => true
      case _ => false
    }
    val wallLocations = levelMap.filter {
      case (_, Wall) => true
      case _ => false
    }.keySet
    for {
      pusherLocation <- getPusherLocation(fields)
      crateLocations = getCrateLocations(fields)
      targetLocations = getTargetLocations(fields)
      if (crateLocations.size == targetLocations.size)
      reachableFields = LevelMap.reachableFields(levelMap, pusherLocation)
      if (crateLocations subsetOf reachableFields)
      if (targetLocations subsetOf reachableFields)
    } yield new Level {
        val map: LevelMap = levelMap 
        val fields: Set[Coord] = reachableFields 
        val walls: Set[Coord] = wallLocations
        val pusher: Coord = pusherLocation
        val crates: Set[Coord] = crateLocations
        val targets: Set[Coord] = targetLocations
      }
  }

  /** Return the pusher location. 
   *  Only valid if map contains exactly one pusher.
   *  @param map the source LevelMap.
   *  @return Some of the pusher location, None otherwise.
   */ 
  private def getPusherLocation(map: LevelMap): Option[Coord] = {
    map.filter {
      case (_, tile) => isFieldWithOccupant(tile, Pusher)
    }.keySet.toList match { // There must be exactly one pusher on a level.
      case h :: Nil => Some(h)
      case _        => None
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

  object LevelMap {
    // Return all fields from m, that are reachable from s.
    def reachableFields(m: LevelMap, s: Coord): Set[Coord] = {
      // Return all neighboring fields for c in map.
      def neighboringFields(c: Coord, map: LevelMap): Set[Coord] = {
        val options = Set(Coord(c.x + 1, c.y), Coord(c.x - 1, c.y), Coord(c.x, c.y + 1), Coord(c.x, c.y - 1))
        options.filter(c =>
          m.get(c) match {
            case Some(Field(_, _)) => true
            case _ => false
          })
      }

      def go(done: Set[Coord], todo: List[Coord]): Set[Coord] = todo match {
        case Nil => done
        case h :: cs =>
          // Add the neighboring fields that have not already been processed.
          val newTodos = neighboringFields(h, m) diff done
          go(done + h, cs ++ newTodos)
      }

      go(Set.empty, List(s))
    }

    // Normalize m, by pushing it as much as possible towards the origin.
    def normalize(m: LevelMap): LevelMap = {
      val coords = m.keySet
      val minX = coords.map(_.x).min
      val minY = coords.map(_.y).min
      if ((minX == 0) && (minY == 0)) m
      else
        m.map { case (c, t) =>
          Coord(c.x - minX, c.y - minY) -> t
        }
    }
  }
}

object AsciiLevelFormat {
  def toLevelMap(ss: List[String]): Option[LevelMap] = {
    // Create map based on all strings in ss.
    val fullMap = ss.zipWithIndex.map {
      case (s, y) =>
        s.zipWithIndex.map {
          case (c, x) => (Coord(x, y), charToTile(c))
        }
    }.flatten.toMap
    val mapOfAllSomeValues = for ((k, Some(v)) <- fullMap) yield k -> v
    if (mapOfAllSomeValues.size == fullMap.size) Some(LevelMap.normalize(mapOfAllSomeValues))
    else None // fullMap contained illegal characters.
  }
  // Convert char (from level file) to a Tile.
  private def charToTile(c: Char): Option[Tile] = c match {
    case '#'           => Some(Wall)
    case '@'           => Some(Field(Pusher, false))
    case '+'           => Some(Field(Pusher, true))
    case '$'           => Some(Field(Crate, false))
    case '*'           => Some(Field(Crate, true))
    case '.'           => Some(Field(Empty, true))
    case ' '| '-'| '_' => Some(Field(Empty, false))
    case _             => None // Illegal character detected.
  }
}