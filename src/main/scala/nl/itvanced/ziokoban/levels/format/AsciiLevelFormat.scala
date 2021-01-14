package nl.itvanced.ziokoban.levels.format

import nl.itvanced.ziokoban.model._
import scala.util.{Failure, Success, Try}

object AsciiLevelFormat {
  import LevelMap._

  def toLevelMap(ss: List[String]): Try[LevelMap] = {
    // Create map based on all strings in ss.
    val fullMap = ss.zipWithIndex
      .map {
        case (s, y) =>
          s.zipWithIndex.map {
            case (c, x) => (Coord(x, y), charToTile(c))
          }
      }
      .flatten
      .toMap
    val mapOfAllSomeValues = for ((k, Some(v)) <- fullMap) yield k -> v
    if (mapOfAllSomeValues.size == fullMap.size) Success(LevelMap.normalize(mapOfAllSomeValues))
    else Failure(new Exception("ASCII specification of level contains illegal character(s)"))
  }

  // Convert char (from level file) to a Tile.
  private def charToTile(c: Char): Option[Tile] =
    c match {
      case '#'             => Some(Wall)
      case '@'             => Some(Field(Pusher, false))
      case '+'             => Some(Field(Pusher, true))
      case '$'             => Some(Field(Crate, false))
      case '*'             => Some(Field(Crate, true))
      case '.'             => Some(Field(Empty, true))
      case ' ' | '-' | '_' => Some(Field(Empty, false))
      case _               => None // Illegal character detected.
    }

}
