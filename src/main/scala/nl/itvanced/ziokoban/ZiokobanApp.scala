package nl.itvanced.ziokoban

import zio.{App, UIO, ZEnv}
import zio.ExitCode
import nl.itvanced.ziokoban.sessionstateaccess.SessionStateAccess

object ZiokobanApp extends App {
  import zio.console.putStrLn
  import nl.itvanced.ziokoban.gameoutput._
  import nl.itvanced.ziokoban.gameinput._
  import nl.itvanced.ziokoban.gameplay._
  import nl.itvanced.ziokoban.levelcollectioncontroller._
  import nl.itvanced.ziokoban.levelcollectionprovider._
  import nl.itvanced.ziokoban.sessionstateaccess._
  import nl.itvanced.ziokoban.config.GameConfig
  import nl.itvanced.ziokoban.gameoutput.ansiconsole.AnsiConsoleOutput
  import zio.ZIO
  import zio.config.syntax._

  /**
   * Implementation of the run method from App.
   *
   * @param args Command line arguments.
   * @return     Final ZIO returning an ExitCode.
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
    val configL                    = GameConfig.asLayer
    val gameInputL                 = JLineGameInput.live
    val gameOutputL                = configL.narrow(_.gameOutput) >>> AnsiConsoleOutput.live
    val gamePlayControllerL        = (gameInputL ++ gameOutputL) >>>  DefaultGamePlayController.live
    val levelsProviderL            = configL.narrow(_.levels) >>> FilesystemLevelCollectionProvider.live
    val sessionStateAccessL        = levelsProviderL >>> DefaultSessionStateAccess.live
    val levelCollectionControllerL = (sessionStateAccessL ++ gamePlayControllerL ++ gameOutputL) >>> DefaultLevelCollectionController.live

    val layers = gameOutputL ++ levelCollectionControllerL

    makeProgram.provideSomeLayer[ZEnv](layers) // Provide all the required layers, except ZEnv.
  }

  val makeProgram: ZIO[GameOutput with LevelCollectionController, Throwable, Unit] = {
    for {
      c <- LevelCollectionController.playLevelCollection()
      _ <- GameOutput.println("Thank you for playing ZIOKOBAN")
    } yield ()
  }
}
