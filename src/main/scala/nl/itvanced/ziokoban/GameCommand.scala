package nl.itvanced.ziokoban

object GameCommands {

  sealed trait GameCommand extends Product with Serializable

  sealed trait MoveCommand    extends GameCommand
  final case object MoveUp    extends MoveCommand
  final case object MoveRight extends MoveCommand
  final case object MoveDown  extends MoveCommand
  final case object MoveLeft  extends MoveCommand
  final case object Undo      extends GameCommand
  final case object Quit      extends GameCommand
  final case object Noop      extends GameCommand
}
