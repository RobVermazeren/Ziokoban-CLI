package nl.itvanced.ziokoban.levels.slc

import scala.util.{Failure, Success, Try}
import scala.xml.XML
import scala.xml.{Node, NodeSeq}
import java.io.File

object SLC {

  def loadFromString(s: String): Try[SlcSokobanLevels] = {
    val rootNode = XML.loadString(s)
    sokobanSlcLevelsFromXML(rootNode)
  }

  def sokobanSlcLevelsFromXML(node: Node): Try[SlcSokobanLevels] =
    for {
      levelCollection <- levelCollectionFromXML((node \ "LevelCollection"))
    } yield {
      val title = (node \ "Title").text
      if (title.isEmpty) throw new Exception("SlcSokobanLevels must contain a Title")
      val description = (node \ "Description").text
      if (description.trim.isEmpty) throw new Exception("SlcSokobanLevels must contain a Decription")
      val trimmedDescription = {
        val trimmedLines = description.lines.toList.map(_.trim)
        val relevantLines =
          trimmedLines
            .dropWhile(_.isEmpty())
            .reverse
            .dropWhile(_.isEmpty())
            .reverse // remove leading and trailing empty lines.
        relevantLines.mkString("\n")
      }
      val email = {
        val raw = (node \ "Email").text
        if (raw.isEmpty) None else Option(raw)
      }
      val url = {
        val raw = (node \ "Url").text
        if (raw.isEmpty) None else Option(raw)
      }
      SlcSokobanLevels(
        title = title,
        description = trimmedDescription,
        email = email,
        url = url,
        collection = levelCollection
      )
    }

  def levelCollectionFromXML(nodes: NodeSeq): Try[SlcLevelCollection] =
    for {
      levels <- getSlcLevelsFromXML(nodes \ "Level")
    } yield {
      val copyright = (nodes \ "@Copyright").text
      if (copyright.isEmpty) throw new Exception("SlcSokobanLevels.SlcLevelCollection must contain a Copyright")
      val maxHeight = {
        val raw = (nodes \ "@MaxHeight").text
        if (raw.isEmpty) None else Option(raw.toInt)
      }
      val maxWidth = {
        val raw = (nodes \ "@MaxWidth").text
        if (raw.isEmpty) None else Option(raw.toInt)
      }
      SlcLevelCollection(
        copyright = copyright,
        maxHeight = maxHeight,
        maxWidth = maxWidth,
        levels = levels
      )
    }

  def getSlcLevelsFromXML(nodes: NodeSeq): Try[List[SlcLevel]] = {
    val allSlcLevels: List[Try[SlcLevel]] = nodes.map(levelFromXML).toList
    if (allSlcLevels.length > 0) {
      val successes: List[SlcLevel]             = allSlcLevels.flatMap(_.toOption)
      val failures: List[Throwable]             = allSlcLevels.flatMap(_.failed.toOption)
      def allErrorMessages(ts: List[Throwable]) = ts.map(_.getMessage()).mkString(" |")
      failures match {
        case Nil => Success(successes)
        case fs  => Failure(new Exception(s"Error reading Levels: ${allErrorMessages(fs)}"))
      }
    } else {
      Failure(new Exception("No levels in collection"))
    }
  }

  def levelFromXML(node: Node): Try[SlcLevel] =
    Try {
      val id = (node \ "@Id").text
      if (id.isEmpty) throw new Exception("SlcSokobanLevels.SlcLevelCollection.SlcLevel must contain an Id")
      val copyright = {
        val raw = (node \ "@Copyright").text
        if (raw.isEmpty) None else Option(raw)
      }
      val height = {
        val raw = (node \ "@Height").text
        if (raw.isEmpty) None else Option(raw.toInt)
      }
      val width = {
        val raw = (node \ "@Width").text
        if (raw.isEmpty) None else Option(raw.toInt)
      }
      val lines = (node \ "L").map(_.text).toList
      if (lines.isEmpty)
        throw new Exception("SlcSokobanLevels.SlcLevelCollection.SlcLevel must contain at least one line")
      SlcLevel(
        id = id,
        copyright = copyright,
        height = height,
        width = width,
        lines = lines
      )
    }

}
