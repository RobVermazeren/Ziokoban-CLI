package nl.itvanced.ziokoban

object Levels {

  val level1: List[String] =
    """    #####
      |    #   #
      |    #$  #
      |  ###  $###
      |  #  $  $ #
      |### # ### #     ######
      |#   # ### #######  ..#
      |# $  $             ..#
      |##### #### #@####  ..#
      |    #      ###  ######
      |    ########
    """.stripMargin.lines.toList

  val invalidLevel = level1.take(6)
}
