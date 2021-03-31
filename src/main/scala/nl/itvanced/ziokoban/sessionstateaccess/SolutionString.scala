package nl.itvanced.ziokoban.sessionstateaccess

import nl.itvanced.ziokoban.model.GameMove
import nl.itvanced.ziokoban.model.Direction

object SolutionString {

  def fromGameMoves(steps: List[GameMove]): String = 
    steps.map(move2string).mkString

  def toGameMoves(solution: String): Option[List[GameMove]] = {

    def loop(s: String, result: List[GameMove]): Option[List[GameMove]] = s.headOption match {
      case None    => Some(result.reverse)
      case Some(c) => toGameMove(c) match {
                        case Some(gm) => loop(s.tail, gm :: result)
                        case None     => None
                      } 
    }
    loop(solution, Nil)
  }

  private def toGameMove(c: Char): Option[GameMove] = c match {
    case 'd' => Some(GameMove(Direction.Down, false))
    case 'D' => Some(GameMove(Direction.Down, true))
    case 'l' => Some(GameMove(Direction.Left, false))
    case 'L' => Some(GameMove(Direction.Left, true))
    case 'r' => Some(GameMove(Direction.Right, false))
    case 'R' => Some(GameMove(Direction.Right, true))
    case 'u' => Some(GameMove(Direction.Up, false))
    case 'U' => Some(GameMove(Direction.Up, true))
    case _   => None 
  }

  /** Translate a Direction to a String */  
  private def move2string(m: GameMove): String = {
    val directionString = m.direction match {
      case Direction.Down  => "d"
      case Direction.Left  => "l"
      case Direction.Right => "r"
      case Direction.Up    => "u"
    }
    if (m.isPush) directionString.toUpperCase else directionString
  }
}
