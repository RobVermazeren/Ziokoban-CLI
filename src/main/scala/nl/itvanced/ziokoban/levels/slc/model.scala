package nl.itvanced.ziokoban.levels.slc

import nl.itvanced.ziokoban.{Level => GeneralLevel}
import nl.itvanced.ziokoban.levels.{LevelCollection => GeneralLevelCollection}
import nl.itvanced.ziokoban.levels.LevelMap
import scala.util.{Failure, Success, Try}
import nl.itvanced.ziokoban.levels.AsciiLevelFormat

case class SokobanLevels(
  title: String,
  description: String,
  email: Option[String],
  url: Option[String],
  collection: LevelCollection
)

object SokobanLevels {
  def toLevelCollection(sls: SokobanLevels): Try[GeneralLevelCollection] = { // RVNOTE: Consider using Validated to collect all errors. (Nice output)
    val convertedLevels = sls.collection.levels.map(convert)

    val failureIndices: List[Int] = convertedLevels.zipWithIndex.collect { case (Failure(e), i) => i } 
    failureIndices match {
      case Nil => // No failures
        Success(
          GeneralLevelCollection(
            title = sls.title,
            description = sls.description,
            levels = convertedLevels.flatMap(_.toOption)
          )
        )

      case idxs => Failure(new Exception(s"Levels with index ${idxs.mkString(",")} are not valid."))
    }
  }

  private def convert(l: Level): Try[GeneralLevel] = for {
    levelMap <- AsciiLevelFormat.toLevelMap(l.lines)
    level    <- GeneralLevel.fromLevelMap(levelMap)
  } yield level
}

case class LevelCollection(
  copyright: String,
  maxWidth: Option[Int],
  maxHeight: Option[Int],
  levels: List[Level]
)

case class Level(
  id: String,
  width: Option[Int],
  height: Option[Int],
  copyright: Option[String],
  lines: List[String]
)