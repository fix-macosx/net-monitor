package babelfish.codecs

import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

import scala.annotation.tailrec
import scalaz.{-\/, \/-, \/}

/**
 * Refer too [[zeroOrMore]]
 */
private[codecs] case class ZeroOrMoreCodec[T] (codec: Codec[T]) extends Codec[List[T]] {
  /** We rely on the list() codec to provide encoding */
  private val listCodec = list(codec)

  override def encode (value: List[T]): \/[String, BitVector] = listCodec.encode(value)
  override def decode (bits: BitVector): \/[String, (BitVector, List[T])] = {
    @tailrec def loop (next: BitVector, parsed: List[T]): \/[String, (BitVector, List[T])] = {
      codec.decode(next) match {
        case \/-((rest, value)) =>
          loop(rest, parsed.:+(value))
        case -\/(err) =>
          /* No more values */
          \/-((next, parsed))
      }
    }

    loop(bits, List.empty)
  }
}