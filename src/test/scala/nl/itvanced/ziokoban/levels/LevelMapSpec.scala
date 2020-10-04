package nl.itvanced.ziokoban.levels

import zio.test._
import Assertion._
import zio.random.Random
import nl.itvanced.ziokoban.Model._
import zio.UIO

object LevelMapSpec extends DefaultRunnableSpec {

  object Generators {
    def coord(minX: Int = 0, maxX: Int = 40, minY: Int = 0, maxY: Int = 40): Gen[Random with Sized, Coord] = for {
      x <- Gen.int(minX, maxX)
      y <- Gen.int(minY, maxY)
    } yield Coord(x, y)
    
    def emptyFieldMap(maxWidth: Int, maxHeight: Int): Gen[Random with Sized, LevelFieldMap] = for {
      width  <- Gen.int(1, maxWidth)
      height <- Gen.int(1, maxHeight)
    } yield {
      val fields = for {
        x <- 1 to width; 
        y <- 1 to height
      } yield {
        Coord(x, y) -> Field(Empty, false)
      }
      fields.toMap
    }
    /** Generate field map, with one pusher */
    def fieldMap(maxWidth: Int, maxHeight: Int, maxCrates: Int): Gen[Random with Sized, LevelFieldMap] = for {
      emptyMap  <- emptyFieldMap(maxWidth, maxHeight)
      map1      <- addPusher(emptyMap) 
      numCrates <- if (maxCrates == 0) Gen.const(0) else Gen.int(1, maxCrates).map(_ min freeFields(map1))
      map2      <- addCrates(map1, numCrates)
      map3      <- addTargets(map2, numCrates)  
    } yield map3

    def freeFields(fm: LevelFieldMap): Int = fm.filter { case (_, field) => field.occupant == Empty }.size

    def emptyTileFromFieldMap(fm: LevelFieldMap): Gen[Random with Sized, (Coord, Field)] = 
      tileFromFieldMap(fm)(f => f.occupant == Empty)

    def nonTargetTileFromFieldMap(fm: LevelFieldMap): Gen[Random with Sized, (Coord, Field)] = 
      tileFromFieldMap(fm)(f => !f.isTarget)

    def tileFromFieldMap(fm: LevelFieldMap)(select: Field => Boolean): Gen[Random with Sized, (Coord, Field)] = {
      val selectedFields = fm.filter{ case (_, f) => select(f) }
      if (selectedFields.isEmpty) {
        Gen.fromEffect(UIO.die(new IllegalArgumentException("no fields comply with selection criteria")))
      } else {
        for {
          i <- Gen.int(1, selectedFields.size)
        } yield {
          selectedFields.drop(i - 1).head // RVNOTE: is there a way to do this without using head?
        }
      }
    }
    
    def addOccupant(fm: LevelFieldMap, occupant: Occupant): Gen[Random with Sized, LevelFieldMap] = for {
      emptyTile <- emptyTileFromFieldMap(fm)
    } yield fm + (emptyTile._1 -> emptyTile._2.copy(occupant = occupant))

    def addPusher(fm: LevelFieldMap): Gen[Random with Sized, LevelFieldMap] = 
      addOccupant(fm, Pusher)

    def addCrate(fm: LevelFieldMap): Gen[Random with Sized, LevelFieldMap] = 
      addOccupant(fm, Crate)

    def addCrates(fm: LevelFieldMap, crateNumber: Int): Gen[Random with Sized, LevelFieldMap] = {
      if (crateNumber < 1) Gen.const(fm)
      else for {
        map       <- addCrate(fm)
        remaining = crateNumber - 1
        map1      <- if (remaining > 0) addCrates(map, remaining)
                     else Gen.const(map) 
      } yield map1
    }

    def addTarget(fm: LevelFieldMap): Gen[Random with Sized, LevelFieldMap] = for {
      nonTargetTile <- nonTargetTileFromFieldMap(fm)
    } yield fm + (nonTargetTile._1 -> nonTargetTile._2.copy(isTarget = true))

    def addTargets(fm: LevelFieldMap, targetNumber: Int): Gen[Random with Sized, LevelFieldMap] = {
      if (targetNumber < 1) Gen.const(fm)
      else for {
        map       <- addTarget(fm)
        remaining = targetNumber - 1
        map1      <- if (remaining > 0) addTargets(map, remaining)
                     else Gen.const(map) 
      } yield map1
    }

    def twoPusherFieldMap(maxWidth: Int, maxHeight: Int, maxCrates: Int = 0): Gen[Random with Sized, LevelFieldMap] = for {
      mapWithPusher <- fieldMap(maxWidth, maxHeight, maxCrates) if mapWithPusher.size > 2 // Ensure room enough for 2 pushers.
      map           <- addPusher(mapWithPusher)
    } yield map

    def growEmptyFieldMap(current: LevelFieldMap, remaining: Int): Gen[Random with Sized, LevelFieldMap] = { 
      if (remaining <= 0) Gen.const(current)
      else {
        for {
          r <- Gen.int(1, 2)
          m <- { if (r == 1) incrementFieldMapHorizontal(current)
                 else        incrementFieldMapVertical(current)
               }
          m1 <- growEmptyFieldMap(m, remaining - 1)     
        } yield {
          m1
        }
      }
    }

    def incrementFieldMapHorizontal(fm: LevelFieldMap): Gen[Random with Sized, LevelFieldMap] = {
      val availableYCoords = fm.keySet.map(_.y).toList 
      for {
        yCoord <- Gen.elements(availableYCoords:_*)
        xCoord <- Gen.elements(emptyHorizontalNeigbours(fm, yCoord):_*) 
      } yield fm + (Coord(xCoord, yCoord) -> Field(Empty, false))
    }

    def incrementFieldMapVertical(fm: LevelFieldMap): Gen[Random with Sized, LevelFieldMap] = {
      val availableXCoords = fm.keySet.map(_.x).toList 
      for {
        xCoord <- Gen.elements(availableXCoords:_*)
        yCoord <- Gen.elements(emptyVerticalNeigbours(fm, xCoord):_*) 
      } yield fm + (Coord(xCoord, yCoord) -> Field(Empty, false))
    }

    def emptyHorizontalNeigbours(fm: LevelFieldMap, y: Int): List[Int] = {
      val existingXsForY = fm.keySet.filter { _.y == y }.map(_.x)
      val allHorizontalNeigbours = existingXsForY.flatMap( x => Set(x - 1, x + 1))
      (allHorizontalNeigbours diff existingXsForY).toList
    }

    def emptyVerticalNeigbours(fm: LevelFieldMap, x: Int): List[Int] = {
      val existingYsForX = fm.keySet.filter { _.x == x }.map(_.y)
      val allVerticalNeigbours = existingYsForX.flatMap( y => Set(y - 1, y + 1))
      (allVerticalNeigbours diff existingYsForX).toList
    }
  }
  
  def spec = suite("LevelMap")(
    getPusherLocationSpec,
    getCrateLocationsSpec,
    getTargetLocationsSpec,
    reachableFieldsSpec,
    normalizeSpec,
  )

  def getPusherLocationSpec = suite(".getPusherLocation")(
    test("empty LevelMap") {
      val result = LevelMap.getPusherLocation(Map.empty)
      assert(result)(isFailure)
    },
    testM("non-empty LevelMap > no pusher") {
      check(Generators.emptyFieldMap(20, 20)) { fieldMap =>
        val result = LevelMap.getPusherLocation(fieldMap)
        assert(result)(isFailure)
      }
    },
    testM("non-empty LevelMap > one pusher") {
      check(Generators.fieldMap(20, 20, 40)) { fieldMap =>
        val result = LevelMap.getPusherLocation(fieldMap)
        assert(result)(isSuccess{
          Assertion.assertion("coord contains pusher")(){ actual => 
            val r = fieldMap.get(actual)
            r.map(_.occupant) == Some(Pusher)
          }
        })
      }
    },
    testM("non-empty LevelMap > multiple pushers") {
      check(Generators.twoPusherFieldMap(20, 20)) { fieldMap =>
        val result = LevelMap.getPusherLocation(fieldMap)
        assert(result)(isFailure)
      }
    },
  )

  def getCrateLocationsSpec = suite(".getCrateLocations")(
    test("empty LevelMap") {
      val result = LevelMap.getCrateLocations(Map.empty)
      assert(result)(isEmpty)
    },
    testM("non-empty LevelMap") {
      check(Generators.fieldMap(maxWidth = 20, maxHeight = 20, maxCrates = 40)) { fieldMap =>
        val resultCoords = LevelMap.getCrateLocations(fieldMap)
        val otherCoords = fieldMap.keySet diff resultCoords

        val resultFields = resultCoords.toList.flatMap(fieldMap.get) // So only fields found for given coords. 
        assert(resultFields.size)(equalTo(resultCoords.size)) // Ensure all resultCoords had corresponding fields.
        assert(resultFields.map(_.occupant))(forall(equalTo(Crate))) // Ensure all resultFields contain crates.

        val otherFields = otherCoords.toList.flatMap(fieldMap.get)
        assert(otherFields.size)(equalTo(otherCoords.size))
        assert(otherFields.map(_.occupant))(forall(not(equalTo(Crate)))) // Ensure all other fields don't contain crates.
      }
    }
  )

  def getTargetLocationsSpec = suite(".getTargetLocations")(
    test("empty LevelMap") {
      val result = LevelMap.getTargetLocations(Map.empty)
      assert(result)(isEmpty)
    },
    testM("non-empty LevelMap") {
      check(Generators.fieldMap(maxWidth = 20, maxHeight = 20, maxCrates = 40)) { fieldMap =>
        val resultCoords = LevelMap.getTargetLocations(fieldMap)
        val otherCoords = fieldMap.keySet diff resultCoords

        val resultFields = resultCoords.toList.flatMap(fieldMap.get) // So only fields found for given coords. 
        assert(resultFields.size)(equalTo(resultCoords.size)) // Ensure all resultCoords had corresponding fields.
        assert(resultFields.map(_.isTarget))(forall(isTrue)) // Ensure all resultFields contain crates.

        val otherFields = otherCoords.toList.flatMap(fieldMap.get)
        assert(otherFields.size)(equalTo(otherCoords.size))
        assert(otherFields.map(_.isTarget))(forall(isFalse)) // Ensure all other fields don't contain crates.
      }
    }
  )

  def reachableFieldsSpec = suite(".reachableFields")(
    testM("non-empty LevelMap (growing algorithm)") {
      val start = Coord(0,0)
      val singleFieldMap = Map(start -> Field(Empty, false))
      check(Generators.growEmptyFieldMap(singleFieldMap, 100)) { fieldMap => 
        val result = LevelMap.reachableFields(fieldMap, start)
        assert(result)(equalTo(fieldMap.keySet))
      }
    } 
  )

  def normalizeSpec = suite(".normalize")(
    test("empty LevelMap") {
      val result = LevelMap.normalize(Map.empty)
      assert(result)(isEmpty)
    },
    testM("one tile LevelMap") {
      check(Generators.coord(-40, 40, -40, 40)) { coord =>
        val map = Map(coord -> Field(Empty, false))
        val result = LevelMap.normalize(map) 
        assert(result.size)(equalTo(1)) 
        val minX = result.keySet.map(_.x).min
        val minY = result.keySet.map(_.y).min
        assert(minX)(equalTo(0)) 
        assert(minY)(equalTo(0)) 
      }
    },
    testM("non-empty LevelMap (growing algorithm)") {
      val start = Coord(0,0)
      val singleFieldMap = Map(start -> Field(Empty, false))
      check(Generators.growEmptyFieldMap(singleFieldMap, 100)) { fieldMap => 
        val result = LevelMap.normalize(fieldMap)
        assert(result.size)(equalTo(101)) 
        val minX = result.keySet.map(_.x).min
        val minY = result.keySet.map(_.y).min
        assert(minX)(equalTo(0)) 
        assert(minY)(equalTo(0)) 
      }
    } 
  )
}
