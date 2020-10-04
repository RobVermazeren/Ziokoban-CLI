package nl.itvanced.ziokoban.levels.slc

import org.scalatest._
import org.scalatest.TryValues._
import java.awt.event.KeyListener

class SLCSpec extends FreeSpec with Matchers {

  object TestCases {

    // Default XML looks like:
    //
    // <?xml version="1.0" encoding="ISO-8859-1"?>
    // <SokobanLevels xsi:schemaLocation="SokobanLev.xsd"
    //    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    //    <Title>Title-1</Title>
    //    <Description>
    //       Description 123.
    //    </Description>
    //    <Email>mail@home.com</Email>
    //    <Url>http://www.domain.com</Url>
    //    <LevelCollection Copyright="copyright 1" MaxWidth="15" MaxHeight="8">
    //       <Level Id="Example 1" Width="15" Height="8" Copyright="copyright 2">
    //          <L>    #####</L>
    //          <L>    #@  #</L>
    //          <L>    # $ #</L>
    //          <L>    #  .#</L>
    //          <L>    #####</L>
    //       </Level>
    //    </LevelCollection>
    // </SokobanLevels>

    private def sokobanLevels(content: List[String]): List[String] = {
      List(
        """<?xml version="1.0" encoding="ISO-8859-1"?>""",
        """<SokobanLevels xsi:schemaLocation="SokobanLev.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">"""
      ) ++
        content ++
        List(
          """</SokobanLevels>"""
        )
    }

    def defaultLevelCollection(): List[String] = {
      List(
        """<LevelCollection Copyright="copyright 1" MaxWidth="15" MaxHeight="8">"""
      ) ++
        defaultLevel() ++
        List(
          """</LevelCollection>"""
        )
    }

    def defaultLevel(): List[String] = {
      List(
        """<Level Id="Example 1" Width="15" Height="8" Copyright="copyright 2">"""
      ) ++
        defaultLines() ++
        List(
          """</Level>"""
        )
    }

    def defaultLines(): List[String] = {
      List(
        """<L>    #####</L>""",
        """<L>    #@  #</L>""",
        """<L>    # $ #</L>""",
        """<L>    #  .#</L>""",
        """<L>    #####</L>"""
      )
    }

    private def attributeString[A](value: Option[A], argumentName: String): String = {
      value.map(v => s""" $argumentName="$v"""").getOrElse("")
    }

    private def tagLines[A](value: Option[A], tagName: String): List[String] = {
      value.map(v => s"""<$tagName>$v</$tagName>""").toList
    }

    def level(
        id: Option[String] = Some("Example 1"),
        copyright: Option[String] = Some("copyright 2"),
        height: Option[Int] = Some(8),
        width: Option[Int] = Some(15),
        lines: () => List[String] = defaultLines
    ): List[String] = {
      val idString        = attributeString(id, "Id")
      val copyrightString = attributeString(copyright, "Copyright")
      val heightString    = attributeString(height, "Height")
      val widthString     = attributeString(width, "Width")
      List(
        s"""<Level$idString$copyrightString$heightString$widthString>"""
      ) ++
        lines() ++
        List(
          "</Level>"
        )

    }

    def levelCollection(
        copyright: Option[String] = Some("copyright 1"),
        maxHeight: Option[Int] = Some(8),
        maxWidth: Option[Int] = Some(15),
        levels: () => List[List[String]] = () => List(defaultLevel())
    ): List[String] = {
      val copyrightString = attributeString(copyright, "Copyright")
      val maxHeightString = attributeString(maxHeight, "MaxHeight")
      val maxWidthString  = attributeString(maxWidth, "MaxWidth")
      List(
        s"""<LevelCollection$copyrightString$maxHeightString$maxWidthString>"""
      ) ++
        levels().flatten ++
        List(
          "</LevelCollection>"
        )
    }

    def create(
        title: Option[String] = Some("Title-1"),
        description: Option[String] = Some("Description 123."),
        email: Option[String] = Some("mail@home.com"),
        url: Option[String] = Some("""http://www.domain.com"""),
        levelCollection: () => List[String] = defaultLevelCollection
    ): String = {
      val titleLines: List[String] = tagLines(title, "Title")
      val descriptionLines: List[String] = description
        .map { d =>
          List(
            Some("<Description>"),
            if (d.isEmpty) None else Some(d),
            Some("</Description>")
          ).flatten
        }
        .toList
        .flatten
      val emailLines: List[String] = tagLines(email, "Email")
      val urlLines: List[String]   = tagLines(url, "Url")

      val content =
        titleLines ++
          descriptionLines ++
          emailLines ++
          urlLines ++
          levelCollection()

      sokobanLevels(content).mkString("\n")
    }

    val fullAllSome = create()

    val fullAllNone =
      create(
        email = None,
        url = None,
        levelCollection = () =>
          levelCollection(
            maxHeight = None,
            maxWidth = None,
            levels = () => List(level(copyright = None, height = None, width = None))
          )
      )

    val invalidCases = List(
      "no title"                           -> create(title = None),
      "empty title"                        -> create(title = Some("")),
      "no description"                     -> create(description = None),
      "empty description"                  -> create(description = Some("")),
      "no level collection"                -> create(levelCollection = () => Nil),
      "level collection without copyright" -> create(levelCollection = () => levelCollection(copyright = None)),
      "level collection without a level"   -> create(levelCollection = () => levelCollection(levels = () => Nil)),
      "level without an id"                -> create(levelCollection = () => levelCollection(levels = () => List(level(id = None)))),
      "level without a line" -> create(levelCollection =
        () => levelCollection(levels = () => List(level(lines = () => Nil)))
      )
    )
  }

  "SLC.loadFromString" - {
    "given a valid slc string (all optional values provided)" - {
      "should return corresponding SlcSokobanLevels instance" in {
        val r             = SLC.loadFromString(TestCases.fullAllSome)
        val sokobanLevels = r.success.value

        sokobanLevels.title shouldBe "Title-1"

        val descriptionLines = sokobanLevels.description.split("\n")
        descriptionLines.length shouldBe 1
        assert(descriptionLines(0) contains "Description 123.")

        sokobanLevels.email.get shouldBe "mail@home.com"
        sokobanLevels.url.get shouldBe "http://www.domain.com"

        val levelCollection = sokobanLevels.collection
        levelCollection.copyright shouldBe "copyright 1"
        levelCollection.maxHeight.get shouldBe 8
        levelCollection.maxWidth.get shouldBe 15
        levelCollection.levels.length shouldBe 1
        levelCollection.levels.headOption match {
          case None => fail("levelCollection must not be empty")
          case Some(h) =>
            h.id shouldBe "Example 1"
            h.copyright.get shouldBe "copyright 2"
            h.height.get shouldBe 8
            h.width.get shouldBe 15
        }
      }
    }

    "given a valid slc string (no optional values provided)" - {
      "should return corresponding SokobanLevels instance" in {
        val r             = SLC.loadFromString(TestCases.fullAllNone)
        val sokobanLevels = r.success.value

        sokobanLevels.title shouldBe "Title-1"

        val descriptionLines = sokobanLevels.description.split("\n")
        descriptionLines.length shouldBe 1
        assert(descriptionLines(0) contains "Description 123.")

        sokobanLevels.email shouldBe None
        sokobanLevels.url shouldBe None

        val levelCollection = sokobanLevels.collection
        levelCollection.copyright shouldBe "copyright 1"
        levelCollection.maxHeight shouldBe None
        levelCollection.maxWidth shouldBe None
        levelCollection.levels.length shouldBe 1
        levelCollection.levels.headOption match {
          case None => fail("levelCollection must not be empty")
          case Some(h) =>
            h.id shouldBe "Example 1"
            h.copyright shouldBe None
            h.height shouldBe None
            h.width shouldBe None
        }
      }
    }

    TestCases.invalidCases.foreach {
      case (descr, testCase) =>
        s"given a invalid slc string ($descr)" - {
          "should return Failure" in {
            val r = SLC.loadFromString(testCase)
            r.failure
          }
        }
    }

  }

}
