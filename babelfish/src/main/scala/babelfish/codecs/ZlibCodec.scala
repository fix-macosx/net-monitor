/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
 *
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

package babelfish.codecs

import java.io.{IOException, ByteArrayOutputStream}
import java.util.zip.{Deflater, DataFormatException, Inflater}

import coop.plausible.nx._
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scalaz.{-\/, \/-, \/}

/**
 * ZLIB compression & decompression.
 */
object ZlibCodec extends Codec[ByteVector] {
  /** @inheritdoc */
  override def decode (bits: BitVector): \/[String, (BitVector, ByteVector)] = try {
    val inflater = new Inflater()
    val inputBytes = bits.toByteArray
    inflater.setInput(inputBytes)

    val output = new ByteArrayOutputStream(inputBytes.length)
    val buffer = new Array[Byte](inputBytes.length.min(1024))
    while (!inflater.finished() && !inflater.needsInput()) {
      val n = inflater.inflate(buffer)
      assertNonThrows[IOException](output.write(buffer, 0, n))
    }

    if (inflater.needsInput()) {
      assertNonThrows[Exception](output.close())
      \/-((BitVector.empty, ByteVector(output.toByteArray)))
    } else {
      -\/(s"Could not decompress file; data underflow occurred")
    }

  } catch {
    case e:DataFormatException => -\/(s"Decompression failed: ${e.getMessage}")
  }

  /** @inheritdoc */
  override def encode (value: ByteVector): \/[String, BitVector] = {
    val deflater = new Deflater()
    val inputBytes = value.toArray
    deflater.setInput(inputBytes)
    deflater.finish()

    val output = new ByteArrayOutputStream(value.length)
    val buffer = new Array[Byte](inputBytes.length.min(1024))
    while (!deflater.finished()) {
      val n = deflater.deflate(buffer)
      assertNonThrows[IOException](output.write(buffer, 0, n))
    }

    assertNonThrows[Exception](output.close())

    \/-(BitVector(output.toByteArray))
  }
}