/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package babelfish.common

import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

import scala.annotation.tailrec
import scalaz.{-\/, \/, \/-}

/**
 * Custom codecs
 */
object Codecs {
  /**
   * Identical to scodec's built-in list() codec, with the exception
   * that a failed decode is not treated as an error; this allows
   * for decoding a list in the case where the list size is not first
   * known.
   *
   * @param codec List element codec.
   * @tparam T List element type.
   */
  case class zeroOrMore[T] (codec: Codec[T]) extends Codec[List[T]] {
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
}
