package nl.itvanced.ziokoban.gameoutput.ansiconsole

case class Config(
  chars: CharConfig
)

case class CharConfig(
  pusherChar: Char,
  crateChar: Char
)

object CharConfig {

  // RVNOTE: ZIO Config does not support Char properties. So for the moment a hack.
  def apply(pusherCharAsString: String, crateCharAsString: String): CharConfig =
    CharConfig(pusherCharAsString.head, crateCharAsString.head)

  def unapply(c: CharConfig): Option[(String, String)] = Some((s"${c.pusherChar}", s"${c.crateChar}"))
}
