package nl.itvanced.ziokoban.levelcollectionprovider

final case class Config(
  directory: String,
  current:   Option[String]
)