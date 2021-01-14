package nl.itvanced.ziokoban

import nl.itvanced.ziokoban.model.{Coord, Field, Tile}

package object levels {

  /** Type alias for a map that represents a Level. */
  type LevelMap = Map[Coord, Tile]

  /** Type alias for a map that represents all field of a Level. */
  type LevelFieldMap = Map[Coord, Field]

}
