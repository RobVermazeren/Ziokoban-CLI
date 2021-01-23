package nl.itvanced

import zio.{App, UIO, ZEnv}
import zio.ExitCode

object ZiokobanApp extends App {
  import zio.console.putStrLn
  import nl.itvanced.ziokoban.{PlayingLevel}
  import nl.itvanced.ziokoban.model.LevelCollection
  import nl.itvanced.ziokoban.gameoutput._
  import nl.itvanced.ziokoban.gameinput._
  import nl.itvanced.ziokoban.gameplay._
  import nl.itvanced.ziokoban.levelcollectionprovider._
  import nl.itvanced.ziokoban.config.GameConfig
  import nl.itvanced.ziokoban.gameoutput.ansiconsole.AnsiConsoleOutput
  import zio.ZIO
  import zio.config.syntax._

  /**
   * Implementation of the run method from App.
   *
   * @param args Command line arguments.
   * @return     Final ZIO returning an ExitCode and with all errors applied.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program()
      .catchSome{
        case e: FilesystemLevelCollectionProviderError => putStrLn(s"Cannot start Ziokoban: ${e.message}")
      }
      .tapError(e => ZIO.succeed(e.printStackTrace()))
      .exitCode

  def program(): ZIO[ZEnv, Throwable, Unit] = {
    // Create instances of all layers and construct the required environment.
    val config              = GameConfig.asLayer
    val gameInputLayer      = JLineGameInput.live
    val gameOutputLayer     = config.narrow(_.gameOutput) >>> AnsiConsoleOutput.live
    val gamePlayLayer       = (gameInputLayer ++ gameOutputLayer) >>>  DefaultGamePlayController.live
    val levelsProviderLayer = config.narrow(_.levels) >>> FilesystemLevelCollectionProvider.live

    val layers = gameOutputLayer ++ gamePlayLayer ++ levelsProviderLayer

    makeProgram.provideSomeLayer[ZEnv](layers) // Provide all the required layers, except ZEnv.
  }

  val makeProgram: ZIO[GameOutput with GamePlayController with LevelCollectionProvider, Throwable, Unit] = {
    for {
      lc <- LevelCollectionProvider.loadLevelCollection()
      // RVNOTE: Need an extra layer here, that takes care of the "playing the collection" 
      //         Logic below will become part of that layer.
      _  <- firstPlayingLevel(lc) match {
        case None =>
          GameOutput.println("This is not a valid level")

        case Some(playingLevel) =>
          for {
            result <- GamePlayController.playLevel(playingLevel)
            _      <- result match {
              case PlayLevelResult.Solved  => GameOutput.println("Congratulations, you won!")
              case PlayLevelResult.Failed  => GameOutput.println("Better luck next time")
              case PlayLevelResult.Aborted => GameOutput.println("Don't give up!") // RVNOTE: Currently not possible
            } 
            _      <- GameOutput.println("Thank you for playing ZIOKOBAN")
          } yield // {
            ()
      }
    } yield ()
  }

  // RVNOTE: This is a temporary method so we can play the first level
  private def firstPlayingLevel(lc: LevelCollection): Option[PlayingLevel] =
    for {
      levelSpec    <- lc.levels.headOption
      playingLevel <- PlayingLevel.fromLevelMap(levelSpec.map).toOption
    } yield playingLevel

}
