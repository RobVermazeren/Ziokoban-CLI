package nl.itvanced.ziokoban.model

sealed trait GameCommand extends Product with Serializable
object GameCommand {

  sealed trait MoveCommand       extends GameCommand
  final case object MoveUp       extends MoveCommand
  final case object MoveRight    extends MoveCommand
  final case object MoveDown     extends MoveCommand
  final case object MoveLeft     extends MoveCommand

  final case object Undo         extends GameCommand
  final case object Replay       extends GameCommand
  final case object Next         extends GameCommand
  final case object NextUnsolved extends GameCommand 
  final case object Previous     extends GameCommand 
  final case object Quit         extends GameCommand
  final case object Noop         extends GameCommand
}
