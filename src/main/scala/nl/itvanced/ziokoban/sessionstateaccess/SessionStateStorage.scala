package nl.itvanced.ziokoban.sessionstateaccess

import zio.Task
import nl.itvanced.ziokoban.model.LevelId

object SessionStateStorage {
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._
 
  /*
    Storage in JSON format.
   */

  def readFromFile(file: os.Path): Task[SessionState] = Task.fromEither {
    val jsonString = os.read(file)
    parser.decode[SessionState](jsonString)
  }

  def writeToFile(file: os.Path, ss: SessionState): Task[Unit] = Task.effect {
    val jsonString = ss.asJson.spaces2
    os.write(file, jsonString)
  }
  
}
