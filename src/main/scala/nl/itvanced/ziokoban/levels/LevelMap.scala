package nl.itvanced.ziokoban.levels

import nl.itvanced.ziokoban.Model._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object LevelMap {
  import nl.itvanced.ziokoban.Model.{Coord, Field, Tile}

  // Return all fields from m, that are reachable from s.
  def reachableFields(m: LevelFieldMap, start: Coord): Set[Coord] =
    reachableFields(m.keySet, start)

  // Return all fields from m, that are reachable from s.
  private def reachableFields(allFields: Set[Coord], s: Coord): Set[Coord] = { // RVHERE: testen schrijven hiervoor. Ergens een fout. (oneindige lus)
    // Return all neighboring fields for c in map.
    def neighboringFields(c: Coord): Set[Coord] = {
      val candidateCoords = Set(Coord(c.x + 1, c.y), Coord(c.x - 1, c.y), Coord(c.x, c.y + 1), Coord(c.x, c.y - 1))
      candidateCoords.filter(c => allFields contains c)
    }

    @tailrec
    def go(done: Set[Coord], todo: List[Coord]): Set[Coord] =
      todo match {
        case Nil => done
        case c :: cs =>
          val newDone = done + c
          // Add the neighboring fields that have not already been processed.
          val newTodos = neighboringFields(c) diff newDone
          go(newDone, (cs.toSet ++ newTodos).toList)
      }

    go(Set.empty, List(s))
  }

  // Normalize m, by pushing it as much as possible towards the origin.
  def normalize(m: LevelMap): LevelMap =
    if (m.isEmpty) m
    else {
      val coords = m.keySet
      val minX   = coords.map(_.x).min
      val minY   = coords.map(_.y).min
      if ((minX == 0) && (minY == 0)) m
      else
        m.map {
          case (c, t) =>
            Coord(c.x - minX, c.y - minY) -> t
        }
    }

  /**
   * Return the pusher location.
   *  Only valid if map contains exactly one pusher.
   *  @param map the source LevelFieldMap.
   *  @return The pusher location, if valid.
   */
  def getPusherLocation(map: LevelFieldMap): Try[Coord] =
    map
      .filter {
        case (_, f) => f.occupant == Pusher
      }
      .keySet
      .toList match { // There must be exactly one pusher on a level.
      case h :: Nil => Success(h)
      case _        => Failure(new Exception("Level should contain exactly one pusher."))
    }

  /**
   * Return all crate locations.
   *  @param map the source LevelFieldMap.
   *  @return A Set of all crate locations.
   */
  def getCrateLocations(map: LevelFieldMap): Set[Coord] =
    map.filter {
      case (_, f) => f.occupant == Crate
    }.keySet

  /**
   * Return all target locations.
   *  @param map the source LevelFieldMap.
   *  @return A Set of all target locations.
   */
  def getTargetLocations(map: LevelFieldMap): Set[Coord] =
    map.filter {
      case (_, f) => f.isTarget
    }.keySet

  def printMap(fm: LevelFieldMap): Unit =
    if (fm.nonEmpty) {
      println("-----------------------------------------------")
      println(s"Size = ${fm.size}")
      println("")
      val xs   = fm.keySet.map(_.x).toList.sorted
      val xMin = xs.min
      val xMax = xs.max
      val ys   = fm.keySet.map(_.y).toList.sorted
      ys.foreach { y =>
        val fieldsForY = fm.filter { case (coord, field) => coord.y == y }
        val xsForY     = fieldsForY.keySet.map(_.x).toList.sorted
        val xsForYMin  = xsForY.min
        val xsForYMax  = xsForY.max

        val preSpaces  = " " * (xsForYMin - xMin)
        val postSpaces = " " * (xMax - xsForYMax)
        val tiles      = (xsForYMin to xsForYMax).map(x => fieldsForY.get(Coord(x, y)).fold(" ")(_ => "█")).mkString

        println(preSpaces + tiles + postSpaces)
      }
    }

}
