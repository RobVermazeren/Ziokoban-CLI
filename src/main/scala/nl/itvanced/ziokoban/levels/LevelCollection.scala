package nl.itvanced.ziokoban.levels

import nl.itvanced.ziokoban.PlayingLevel

case class LevelCollection(
  title: String,
  description: String,
  levels: List[PlayingLevel]
)
