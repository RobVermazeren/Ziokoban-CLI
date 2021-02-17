package nl.itvanced.ziokoban.model

import nl.itvanced.ziokoban.model.LevelMap

case class LevelCollection( 
  title: String,
  description: String,
  levels: Vector[LevelSpec]
)

case class LevelSpec(
  id: LevelId,
  map: LevelMap
)

class LevelId(val value: String) extends AnyVal
