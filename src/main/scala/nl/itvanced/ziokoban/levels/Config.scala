package nl.itvanced.ziokoban.levels

final case class Config(
  directory: String,
  current:   Option[String]
)