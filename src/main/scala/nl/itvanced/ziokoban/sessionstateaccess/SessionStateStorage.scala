package nl.itvanced.ziokoban.sessionstateaccess

import zio.{Task, UIO}
import nl.itvanced.ziokoban.model.LevelId
import java.io.FileWriter

object SessionStateStorage {
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._
 
  /*
    Storage in JSON format.
   */
  def readFromFile(file: os.Path): Task[SessionState] = 
    createIoSource(file).bracket(closeIoSource){
      source => 
        Task.fromEither {
          val jsonString = source.getLines().mkString
          parser.decode[SessionState](jsonString)
        }
    }

  def writeToFile(file: os.Path, ss: SessionState): Task[Unit] = 
    createFileWriter(file).bracket(closeFileWriter){
      writer =>
        Task.effect { 
          val jsonString = ss.asJson.spaces2
          writer.write(jsonString)
        }
    }

  private def createIoSource(file: os.Path): Task[scala.io.Source] =
    Task.effect(scala.io.Source.fromInputStream(file.getInputStream))  
  
  private def closeIoSource(source: scala.io.Source): UIO[Unit] =
    UIO.effectTotal(source.close()) 

  private def createFileWriter(file: os.Path): Task[FileWriter] = 
    Task.effect(new FileWriter(file.toIO))  

  private def closeFileWriter(writer: FileWriter): UIO[Unit] = 
    UIO.effectTotal(writer.close())  
}
