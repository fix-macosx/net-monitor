package babelfish.common

import java.nio.charset.StandardCharsets

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import scodec.Codec
import scodec.bits.BitVector

import scalaz._

class StringCodecsTest extends Specification {
  val charset = StandardCharsets.UTF_8
  val strings = new StringCodecs(charset)

  protected def roundtrip[A] (codec: Codec[A], value: A): MatchResult[Any] = {
    val encoded = codec.encode(value)
    encoded.toOption should beSome

    val \/-((remainder, decoded)) = codec.decode(encoded.toOption.get)
    remainder must beEqualTo(BitVector.empty)
    decoded must beEqualTo(value)
  }

  "regex parsers" should {
    "roundtrip" in {
      roundtrip(strings.regex(".*".r), "Hello World")
    }
  }

}
