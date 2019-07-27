package nl.itvanced.ziokoban.gameoutput.ansiconsole

object ConsoleDrawing {
  import cats.data.State
  import nl.itvanced.ziokoban.Model.Coord
  import org.fusesource.jansi.Ansi
  import org.fusesource.jansi.Ansi.Attribute
  import org.fusesource.jansi.Ansi.{ Color => AnsiColor}

  type ConsoleDrawState[A] = State[Ansi, A]

  sealed trait BaseColor
  object BaseColor {
    final object Black extends BaseColor
    final object Red extends BaseColor
    final object Green extends BaseColor
    final object Yellow extends BaseColor
    final object Blue extends BaseColor
    final object Magenta extends BaseColor
    final object Cyan extends BaseColor
    final object White extends BaseColor
  }

  private def toAnsiColor(c: BaseColor): AnsiColor = c match { 
    case BaseColor.Black => Ansi.Color.BLACK
    case BaseColor.Red => Ansi.Color.RED
    case BaseColor.Green => Ansi.Color.GREEN
    case BaseColor.Yellow => Ansi.Color.YELLOW
    case BaseColor.Blue => Ansi.Color.BLUE
    case BaseColor.Magenta => Ansi.Color.MAGENTA
    case BaseColor.Cyan => Ansi.Color.CYAN
    case BaseColor.White => Ansi.Color.WHITE
  }

  case class Color(baseColor: BaseColor, isBright: Boolean)
  object Color {
    val IsBright = true
    val DefaultFg = Color(BaseColor.White, IsBright)
    val DefaultBg = Color(BaseColor.Black, !IsBright)
    val Border = Color(BaseColor.White, IsBright)
  }

  object Draw {
    def eraseScreen: ConsoleDrawState[Unit] = State.modify { _.eraseScreen() }
    def saveCursorPosition: ConsoleDrawState[Unit] = State.modify { _.saveCursorPosition() }
    def restoreCursorPosition: ConsoleDrawState[Unit] = State.modify { _.restoreCursorPosition() }

    def cursorToPosition(row: Int, column: Int): ConsoleDrawState[Unit] = State.modify { _.cursor(row, column) }
    def hideCursor: ConsoleDrawState[Unit] = State.modify { _.format("\u001B[?25l") }
    def showCursor: ConsoleDrawState[Unit] = State.modify { _.format("\u001B[?25h") }

    def drawBorder(x0: Int, y0: Int, width: Int, height: Int): ConsoleDrawState[Unit] = State.modify { ansi =>
      val topLine = "┌".concat("─" * (width - 2)).concat("┐")
      val sideLines = "│".concat(" " * (width - 2)).concat("│")
      val bottomLine = "└".concat("─" * (width - 2)).concat("┘")
      val top = ansi.cursor(y0, x0).a(topLine)
      val sides = (0 to height - 2).toList.foldLeft(top) { (bb: Ansi, i: Int) =>
        bb.cursor(y0 + i + 1, x0).a(sideLines)
      }
      sides.cursor(y0 + height - 1, x0).a(bottomLine)
    }

    def setForeGroundColor(fg: Color): ConsoleDrawState[Unit] = State.modify { ansi =>
      if (fg.isBright)
        ansi.fgBright(toAnsiColor(fg.baseColor))
      else
        ansi.fg(toAnsiColor(fg.baseColor))
    }

    def setBackGroundColor(bg: Color): ConsoleDrawState[Unit] = State.modify { ansi =>
      if (bg.isBright)
        ansi.bgBright(toAnsiColor(bg.baseColor))
      else
        ansi.bg(toAnsiColor(bg.baseColor))
    }

    def drawChar(position: Coord, c: Char): ConsoleDrawState[Unit] = drawChars(Set(position), c)
    def drawChars(positions: Set[Coord], c: Char): ConsoleDrawState[Unit] = State.modify { ansi =>
      positions.foldLeft(ansi) { case (rAnsi, pos) =>
        rAnsi.cursor(pos.y, pos.x).a(c) }
    }
    def drawAttribute(attribute: Attribute): ConsoleDrawState[Unit] = State.modify { _.a(attribute) }
  }
}
