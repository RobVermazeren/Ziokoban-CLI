package nl.itvanced.ziokoban.levelsProvider

final case class Config(
  directory: String,
  current:   Option[String]
)