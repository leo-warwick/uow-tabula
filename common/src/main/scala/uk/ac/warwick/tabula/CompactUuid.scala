package uk.ac.warwick.tabula

import java.math.BigInteger
import scala.util.matching.Regex
import org.apache.commons.codec.binary.Base64

/**
 * Reformats a long UUID string style into the shortest possible ASCII representation,
 * by parsing it into its byte value and encoding as Base64.
 *
 * When I say shortest possible - you might be able to get it shorter by finding
 * a few more ASCII characters to use, but probably not many that wouldn't get URL
 * encoded (since the main purpose of this class would be to shorten URLs).
 *
 * This object isn't currently used, but it is fun to have around.
 */
object CompactUuid {

	val UuidSplit = new Regex("(.{8})(.{4})(.{4})(.{4})(.{12})")

	val BASE = 16

	def uncompact(compactUuid: String): Option[String] =
		try {
			decode64(compactUuid).toString(BASE) match {
				case UuidSplit(a, b, c, d, e) => Some("%s-%s-%s-%s-%s".format(a, b, c, d, e))
				case _ => None
			}
		} catch {
			case e: NumberFormatException => None
		}

	def compact(uuid: String): Option[String] =
		try {
			Some(encode64(new BigInteger(uuid.replace("-", ""), BASE)))
		} catch {
			case e: NumberFormatException => None
		}

	private def encode64(bigint: BigInteger) =
		new String(Base64.encodeBase64(bigint.toByteArray)).replace("=", "")

	private def decode64(string: String) =
		new BigInteger(Base64.decodeBase64(string))

}