package nl.itvanced.ziokoban.config

import nl.itvanced.ziokoban.gameoutput.ansiconsole.{Config => GameOutputConfig}
import zio.{Task, ZIO}

case class GameConfig(
  gameOutput: GameOutputConfig  
)

object GameConfig {
  import pureconfig.generic.ProductHint
  import pureconfig.generic.auto._
  import pureconfig._

  implicit def productHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  def load(): Task[GameConfig] = ZIO.effect[GameConfig](pureconfig.loadConfigOrThrow[GameConfig])
}
