package nl.itvanced.ziokoban.gameoutput.ansiconsole

case class Config(
    chars: CharConfig
)

case class CharConfig(
    pusherChar: Char,
    crateChar: Char
)
