

/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
 *
 * Portions of this file were derived from the Scodec project:
 *
 * Copyright (c) 2013-2014, Michael Pilquist and Paul Chiusano
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the scodec team nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package babelfish

import java.nio.charset.Charset

import scodec.Codec
import scodec.bits.ByteVector

import scala.util.matching.Regex

/**
 * Codec Definitions.
 */
package object codecs {
  /**
   * Codec that performs base64 encoding and decoding on all available input data.
   *
   * @param scheme The base64 encoding scheme to be used by the returned codec.
   */
  def base64 (scheme: Base64Codec.Scheme): Codec[ByteVector] = Base64Codec(scheme)

  /**
   * Codec that performs regex-based matching on input data. Matching data is consumed until
   * the regex fails to match, or the input data cannot be decoded using the provided
   * character set.
   *
   * @param regex The regex to be used for parsing -- and when validating a value to be encoded.
   * @param charset The character set of the input data.
   *
   * Syntax extensions to facilitate working with regex codecs are available via [[RegexCodec.syntax]].
   */
  def regex (regex: Regex, charset: Charset): Codec[String] = RegexCodec(regex, charset)

  /**
   * Codec that encodes/decodes a `List[A]` from a `Codec[A]`.
   *
   * When encoding, each `A` in the list is encoded and all of the resulting vectors are concatenated.
   *
   * When decoding, `codec.decode` is called repeatedly until there are no more remaining bits, or an error occurs.
   * The value result of each successful `decode` is returned in the list.
   *
   * This is identical to scodec's built-in list() codec, with the exception
   * that a failed decode is not treated as an error.
   *
   * @param codec codec to encode/decode a single element of the sequence
   */
  def zeroOrMore[T] (codec: Codec[T]): Codec[List[T]] = ZeroOrMoreCodec(codec)


  /**
   * Codec that performs zlib-based compression and decompression of all available input.
   */
  def zlib: Codec[ByteVector] = ZlibCodec
}
