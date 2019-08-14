package nl.itvanced.ziokoban

object Model {
  sealed trait Direction
  object Direction {
    final case object Up extends Direction
    final case object Right extends Direction
    final case object Down extends Direction
    final case object Left extends Direction
  }

  sealed trait Occupant
  case object Empty extends Occupant
  case object Pusher extends Occupant
  case object Crate extends Occupant

  sealed trait Tile
  case object Wall extends Tile
  case object Void extends Tile // outside of walls
  case class Field(occupant: Occupant, isTarget: Boolean) extends Tile

  def isFieldWithOccupant(tile: Tile, occupant: Occupant): Boolean = tile match {
    case Field(o, _) if o == occupant => true
    case _                            => false
  }

  case class Coord(x: Int, y: Int)
}
