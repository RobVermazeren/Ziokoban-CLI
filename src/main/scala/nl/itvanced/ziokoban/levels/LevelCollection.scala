package nl.itvanced.ziokoban.levels

import nl.itvanced.ziokoban.{Level => GeneralLevel}

case class LevelCollection(
  title: String,
  description: String,
  levels: List[GeneralLevel]
)
