package nl.itvanced.ziokoban.levels

import nl.itvanced.ziokoban.model.LevelMap

case class LevelCollection( // RVNOTE: Move this out of levels
  title: String,
  description: String,
  levels: List[LevelSpec]
)

case class LevelSpec(
  id: String,
  map: LevelMap
)
