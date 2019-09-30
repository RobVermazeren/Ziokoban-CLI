package nl.itvanced.ziokoban.levels.slc

case class SokobanLevels(
  title: String,
  description: String,
  email: Option[String],
  url: Option[String],
  collection: LevelCollection
)

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