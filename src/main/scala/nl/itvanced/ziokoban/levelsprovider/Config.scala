package nl.itvanced.ziokoban.levelsprovider

final case class Config(
  directory: String,
  current:   Option[String]
)