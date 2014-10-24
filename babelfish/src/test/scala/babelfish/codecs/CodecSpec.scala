package babelfish.codecs

import org.specs2.mutable.SpecificationLike
import org.specs2.matcher.MatchResult
import scodec.Codec
import scodec.bits.BitVector

import scalaz.\/-

/**
 * Common codec testing behavior.
 */
trait CodecSpec { self:SpecificationLike =>

  /**
   * Perform round-trip encoding and decoding of the given value, ensuring
   * that all bytes are consumed and the result is equal (using a custom match
   * function).
   *
   * @param codec The codec to be used.
   * @param value The value to be encoded.
   * @param eq The function to call to determine equality.
   * @tparam A The value's type.
   * @return A successful match result if round-trip encoding succeeds.
   */
  protected def roundtrip[A] (codec: Codec[A], value: A, eq: (A, A) => MatchResult[Any]): MatchResult[Any] = {
    val encoded = codec.encode(value)
    encoded.toOption should beSome

    val \/-((remainder, decoded)) = codec.decode(encoded.toOption.get)
    remainder must beEqualTo(BitVector.empty)
    eq(decoded, value)
  }

  /**
   * Perform round-trip encoding and decoding of the given value, ensuring
   * that all bytes are consumed and the result is equal.
   *
   * @param codec The codec to be used.
   * @param value The value to be encoded.
   * @tparam A The value's type.
   * @return A successful match result if round-trip encoding succeeds.
   */
  protected def roundtrip[A] (codec: Codec[A], value: A): MatchResult[Any] = {
    val encoded = codec.encode(value)
    encoded.toOption should beSome

    val \/-((remainder, decoded)) = codec.decode(encoded.toOption.get)
    remainder must beEqualTo(BitVector.empty)
    decoded must beEqualTo(value)
  }

}
