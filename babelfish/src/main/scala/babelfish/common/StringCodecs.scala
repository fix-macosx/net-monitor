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

import java.nio.CharBuffer
import java.nio.charset.{Charset, CodingErrorAction}

import scodec.Codec
import scodec.bits.BitVector

import scala.annotation.tailrec
import scala.util.matching.Regex
import scalaz.{-\/, \/, \/-}

/**
 * String-based codecs.
 * @param charset The charset to use for all encoding/decoding.
 */
class StringCodecs (charset: Charset) {

  /**
   * String syntax extensions.
   */
  object syntax {
    /**
     * Provides implicit conversion from a regex to a regex-based codec.
     * @param r The regex to wrap.
     */
    implicit def regexCodec (r: Regex): Codec[String] = regex(r)
  }

  /**
   * Regex-based codec
   *
   * @param regex The regex to be used for parsing -- and when validating a value to be encoded.
   */
  case class regex (regex: Regex) extends Codec[String] {
    /** @inheritdoc */
    override def encode (value: String): \/[String, BitVector] = {
      if (regex.pattern.matcher(value).matches()) {
        \/-(BitVector(charset.encode(value)))
      } else {
        -\/(s"$value does not match regex $regex")
      }
    }

    /** @inheritdoc */
    override def decode (bits: BitVector): \/[String, (BitVector, String)] = {
      /* This ugly inefficient monster implements byte-at-a-time string decoding + regex matching
       * on top of non-streaming regex libraries. */
      val decoder = charset.newDecoder().reset().onMalformedInput(
        CodingErrorAction.REPORT
      ).onUnmappableCharacter(
        CodingErrorAction.REPORT
      )

      @tailrec def loop (next: BitVector, hasMatched: Boolean, matched: String, byteLength: Long): \/[String, (BitVector, String)] = {
        def produceResult = if (hasMatched) {
          val remainder = bits.drop(byteLength * 8)
          \/-((remainder, matched))
        } else {
          -\/(s"input does not match regex '$regex'")
        }

        if (next.isEmpty) {
          /* If we hit the end, return a match if we have one */
          produceResult
        } else {
          /* Try reading one byte; this may underflow. */
          val cbuf = CharBuffer.allocate(2)
          val result = decoder.decode(next.take(8).toByteBuffer, cbuf, next.length <= 8)
          if (next.length <= 8) {
            decoder.flush(cbuf)
          }

          if (result.isError) {
            produceResult
          } else if (cbuf.length() > 0) {
            cbuf.flip()
            val decoded = new StringBuilder(matched).append(cbuf).toString()
            val matcher = regex.pattern.matcher(decoded)
            val doesMatch = matcher.find() && matcher.start == 0 && matcher.end == decoded.length

            /* Check if the new data matches. If so, we should continue. If not, and
             * we previously matched, we're now finished */
            if (doesMatch || !hasMatched) {
              loop(next.drop(8), doesMatch, decoded, byteLength + 1)
            } else {
              produceResult
            }
          } else {
            /* Underflow, keep going */
            loop(next.drop(8), false, matched, byteLength + 1)
          }
        }
      }

      // println("START")
      loop(bits, false, "", 0)
    }
  }
}
