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

import java.util.Base64
import java.util.Base64.{Encoder, Decoder}

import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scalaz.{-\/, \/-, \/}

/**
 * Base64 Scheme
 */
sealed trait Base64Scheme {
  /** Return a new base64 encoder for this scheme */
  def newEncoder: Base64.Encoder

  /** Return a new base64 decoder for this scheme */
  def newDecoder: Base64.Decoder
}

/**
 * Supported Base64 Schemes.
 */
object Base64Scheme {
  /**
   * MIME Base64 Scheme
   */
  object MIME extends Base64Scheme {
    override def newEncoder: Encoder = Base64.getMimeEncoder
    override def newDecoder: Decoder = Base64.getMimeDecoder
  }
}

/**
 * Base64 Codec
 */
case class Base64Codec (scheme: Base64Scheme) extends Codec[ByteVector] {
  override def decode (bits: BitVector): \/[String, (BitVector, ByteVector)] = try {
    \/-((BitVector.empty, ByteVector(scheme.newDecoder.decode(bits.toByteArray))))
  } catch {
    case ia: IllegalArgumentException => -\/(s"Could not decode the input data as base64: ${ia.getMessage}")
  }

  override def encode (value: ByteVector): \/[String, BitVector] = {
    \/-(BitVector(scheme.newEncoder.encode(value.toArray)))
  }
}
