package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.core.{StringUtils => Utils}

import scala.language.implicitConversions

/**
  * Scala-style String utilities. Adds the methods as implicit methods
  * on String.
  */
trait StringUtils {

  class SuperString(string: String) {
    def hasText: Boolean = Utils hasText string
    def isEmptyOrWhitespace: Boolean = !hasText
    def hasLength: Boolean = Utils hasLength string
    def safeTrim: String = Option(string).map(_.trim).getOrElse("")
    def safeSubstring(proposedStart: Int): String = Utils.safeSubstring(string, proposedStart)
    def safeSubstring(proposedStart: Int, proposedEnd: Int): String = Utils.safeSubstring(string, proposedStart, proposedEnd)
    def orEmpty: String = Option(string).getOrElse("")
    def maybeText: Option[String] = Option(string).filter(Utils.hasText)
    def textOrEmpty: String = maybeText.getOrElse("")
    def safeLowercase: String = Option(string).map(_.toLowerCase).getOrElse("")
    def safeLength: Int = Option(string).fold(0)(_.length)
    def safeContains(substring: String): Boolean = Option(string).exists(_.contains(substring))
    def safeStartsWith(substring: String): Boolean = Option(string).exists(_.startsWith(substring))
  }

  implicit def StringToSuperString(string: String) = new SuperString(string)

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

}

object StringUtils extends StringUtils {

  // From http://www.davekoelle.com/files/AlphanumComparator.java
  object AlphaNumericStringOrdering extends Ordering[String] {

    /** Length of string is passed in for improved efficiency (only need to calculate it once) **/
    private def getChunk(s: String, slength: Int, initialMarker: Int) = {
      val chunk = new StringBuilder

      var marker = initialMarker
      var c = s.charAt(marker)

      chunk.append(c)
      marker += 1

      val isDigit = Character.isDigit(c)
      while (marker < slength && Character.isDigit(s.charAt(marker)) == isDigit) {
        c = s.charAt(marker)
        chunk.append(c)
        marker += 1
      }

      chunk.toString()
    }

    private def compareStrings(s1: String, s2: String) = {
      var s1Marker = 0
      var s2Marker = 0
      val s1Length = s1.safeLength
      val s2Length = s2.safeLength

      var result = 0
      while (s1Marker < s1Length && s2Marker < s2Length && result == 0) {
        val s1Chunk = getChunk(s1, s1Length, s1Marker)
        s1Marker += s1Chunk.length

        val s2Chunk = getChunk(s2, s2Length, s2Marker)
        s2Marker += s2Chunk.length

        // If both chunks contain numeric characters, sort them numerically
        if (Character.isDigit(s1Chunk.charAt(0)) && Character.isDigit(s2Chunk.charAt(0))) {
          // Simple chunk comparison by length
          val s1ChunkLength = s1Chunk.length
          result = s1ChunkLength - s2Chunk.length

          // If equal, the first different number counts
          if (result == 0) {
            for (i <- 0 until s1ChunkLength; if result == 0) {
              result = s1Chunk.charAt(i) - s2Chunk.charAt(i)
            }
          }
        } else {
          result = s1Chunk compare s2Chunk
        }
      }

      result
    }

    def compare(a: String, b: String): Int = {
      compareStrings(a.safeLowercase, b.safeLowercase)
    }

  }

}
