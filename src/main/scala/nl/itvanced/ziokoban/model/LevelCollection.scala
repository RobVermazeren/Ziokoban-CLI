package nl.itvanced.ziokoban.model

import nl.itvanced.ziokoban.model.LevelMap

case class LevelCollection( 
  title: String,
  description: String,
  levels: Vector[LevelSpec]
)

case class LevelSpec(
  id: String,
  map: LevelMap
)
