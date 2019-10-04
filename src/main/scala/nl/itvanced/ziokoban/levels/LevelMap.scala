package nl.itvanced.ziokoban.levels

object LevelMap {
  import nl.itvanced.ziokoban.Model.{Coord, Field, Tile}

  /** Type alias for a map that represents a Level. */
  type LevelMap = Map[Coord, Tile]

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