package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.Level.LevelMap
import nl.itvanced.ziokoban.Model._

case class Level private (map: Level.LevelMap, spaces: Set[Coord], walls: Set[Coord], pusherLocation: Coord, crateLocations: Set[Coord], targetLocations: Set[Coord]) {
  val height: Int = map.keySet.map(_.y).max + 1
  val width: Int = map.keySet.map(_.x).max + 1
}

object Level {
  type LevelMap = Map[Coord, Tile]

  // Create a level for this map. Return None, if map does not define a valid level.
  def apply(map: LevelMap): Option[Level] = { 
    val spaces = map.filter {
      case (_, Space(_, _)) => true
      case _ => false
    }
    val walls = map.filter {
      case (_, Wall) => true
      case _ => false
    }.keySet
    for {
      pusherLocation <- getPusherLocation(spaces)
      crateLocations = getCrateLocations(spaces)
      targetLocations = getTargetLocations(spaces)
      if (crateLocations.size == targetLocations.size)
      reachableSpaces = LevelMap.reachableSpaces(map, pusherLocation)
      if (crateLocations subsetOf reachableSpaces)
      if (targetLocations subsetOf reachableSpaces)
    } yield new Level(map, reachableSpaces, walls, pusherLocation, crateLocations, targetLocations)
  }

  // Return the pusher location from map. Return None if no or more than one pusher have been defined.
  private def getPusherLocation(map: LevelMap): Option[Coord] = {
    map.filter {
      case (_, tile) => isSpaceWithOccupant(tile, Pusher)
    }.keySet.toList match { // There must be exactly one pusher on a level.
      case h :: Nil => Some(h)
      case _        => None
    }
  }

  // Return all crate locations from map.
  private def getCrateLocations(map: LevelMap): Set[Coord] = {
    map.filter {
      case (_, tile) => isSpaceWithOccupant(tile, Crate)
    }.keySet
  }

  // Return all target locations from map.
  private def getTargetLocations(map: LevelMap): Set[Coord] = {
    map.filter {
      case (_, Space(_, true)) => true
      case _                   => false
    }.keySet
  }

  object LevelMap {
    // Return all spaces from m, that are reachable from s.
    def reachableSpaces(m: LevelMap, s: Coord): Set[Coord] = {
      // Return all neighboring spaces for c in map.
      def neighboringSpaces(c: Coord, map: LevelMap): Set[Coord] = {
        val options = Set(Coord(c.x + 1, c.y), Coord(c.x - 1, c.y), Coord(c.x, c.y + 1), Coord(c.x, c.y - 1))
        options.filter(c =>
          m.get(c) match {
            case Some(Space(_, _)) => true
            case _ => false
          })
      }

      def go(done: Set[Coord], todo: List[Coord]): Set[Coord] = todo match {
        case Nil => done
        case h :: cs =>
          // Add the neighboring spaces that have not already been processed.
          val newTodos = neighboringSpaces(h, m) diff done
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
    case '@'           => Some(Space(Pusher, false))
    case '+'           => Some(Space(Pusher, true))
    case '$'           => Some(Space(Crate, false))
    case '*'           => Some(Space(Crate, true))
    case '.'           => Some(Space(Empty, true))
    case ' '| '-'| '_' => Some(Space(Empty, false))
    case _             => None // Illegal character detected.
  }
}