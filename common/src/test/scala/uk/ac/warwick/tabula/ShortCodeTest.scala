package uk.ac.warwick.tabula

import scala.collection.immutable.SortedSet
import scala.math.max

// scalastyle:off magic.number
class ShortCodeTest extends TestBase {
  @Test def randomShortCode: Unit = {
    ShortCode.random() should fullyMatch regex """[a-zA-Z0-9]{5}"""
  }

  /**
    * Check that we can generate a bunch of unique codes with only
    * a few retries in case of collision.
    */
  @Test(timeout = 10000) def roughUniqueness: Unit = {
    val number = 100000
    var generated = SortedSet[String]()
    var collisions = 0;
    var maxIndividualCollisions = 0
    for (i <- 1 to number) {
      var individualCollisions = 0
      var code = ShortCode.random()
      while (generated.contains(code)) {
        code = ShortCode.random()
        individualCollisions += 1
      }
      collisions += individualCollisions
      maxIndividualCollisions = max(maxIndividualCollisions, individualCollisions)
      generated += code
    }
    println("Generated %d codes with %d collisions (max %d retries for one item)".format(number, collisions, maxIndividualCollisions))
  }
}
