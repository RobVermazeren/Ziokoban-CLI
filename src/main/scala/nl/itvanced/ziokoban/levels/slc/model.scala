package nl.itvanced.ziokoban.levels.slc

import nl.itvanced.ziokoban.PlayingLevel
import nl.itvanced.ziokoban.model.LevelCollection
import nl.itvanced.ziokoban.model.LevelMap
import scala.util.{Failure, Success, Try}
import nl.itvanced.ziokoban.levels.format.AsciiLevelFormat
import nl.itvanced.ziokoban.model.LevelSpec

case class SlcSokobanLevels(
  title: String,
  description: String,
  email: Option[String],
  url: Option[String],
  collection: SlcLevelCollection
)

object SlcSokobanLevels {

  def toLevelCollection(sls: SlcSokobanLevels): Try[LevelCollection] = { // RVNOTE: Consider using Validated to collect all errors. (Nice output)
    val convertedLevels: List[Try[LevelSpec]] = sls.collection.levels.map(convert)
    val failureIndices: List[Int] = convertedLevels.zipWithIndex.collect { case (Failure(e), i) => i }
    failureIndices match {
      case Nil => // No failures
        Success(
          LevelCollection(
            title = sls.title,
            description = sls.description,
            levels = convertedLevels.flatMap(_.toOption)
          )
        )

      case idxs => Failure(new Exception(s"Levels with index ${idxs.mkString(",")} are not valid."))
    }
  }

  private def convert(l: SlcLevel): Try[LevelSpec] =
    for {
      levelMap <- AsciiLevelFormat.toLevelMap(l.lines)
    } yield
      LevelSpec(id = l.id, map = levelMap)

}

case class SlcLevelCollection(
  copyright: String,
  maxWidth: Option[Int],
  maxHeight: Option[Int],
  levels: List[SlcLevel]
)

case class SlcLevel(
  id: String,
  width: Option[Int],
  height: Option[Int],
  copyright: Option[String],
  lines: List[String]
)
