package nl.itvanced.ziokoban.config

import nl.itvanced.ziokoban.gameoutput.ansiconsole.{Config => GameOutputConfig, CharConfig => GameOutputCharConfig}
import zio.{Task, ZIO}
import zio.config._, ConfigDescriptor._, ConfigSource._
import zio.config.typesafe.TypesafeConfigSource._
import zio.config.typesafe.TypesafeConfig

case class GameConfig(
    gameOutput: GameOutputConfig
)

object GameConfig {
  // Manual creation of config descriptor, in order to get better understanding of what is going on.
  val gameOutputCharsConfigDescr = 
    (string("pusherChar") |@| string("crateChar"))(GameOutputCharConfig.apply, GameOutputCharConfig.unapply) 
  
  val gameOutputConfigDescr = 
    (nested("chars") {gameOutputCharsConfigDescr})(GameOutputConfig.apply, GameOutputConfig.unapply)

  val gameConfigDescr =
    (nested("gameOutput") {gameOutputConfigDescr})(GameConfig.apply, GameConfig.unapply) 

  def asLayer = {
    TypesafeConfig.fromHoconFile(new java.io.File("./ziokoban.conf"), gameConfigDescr) <> // = orElse
    TypesafeConfig.fromDefaultLoader(gameConfigDescr)

  }
}
