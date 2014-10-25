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

package babelfish.codecs

import java.io.IOException
import java.nio.charset.StandardCharsets

import coop.plausible.nx._
import org.bouncycastle.cms.{CMSSignedData, CMSException}
import org.specs2.mutable.Specification
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

import scalaz.\/-

class CodecsTest extends Specification with CodecSpec {
  "base64 mime" should {
    "roundtrip" in {
      roundtrip(base64(Base64Codec.MIME), ByteVector("Hello, World".getBytes(StandardCharsets.UTF_8)))
    }
  }

  "cms.signedData" should {
    "roundtrip" in {
      val is = CodecsTest.this.getClass.getResourceAsStream("CMSSignedDataExample")
      (is must not).beNull

      val signedData = assertNonThrows[CMSException] (new CMSSignedData(is))

      roundtrip(cms.signedData, signedData, { (a:CMSSignedData, b:CMSSignedData) =>
        assertNonThrows[IOException] {
          a.toASN1Structure.getEncoded must beEqualTo(b.toASN1Structure.getEncoded)
        }
      })
    }
  }

  "regex parsers" should {
    "roundtrip" in {
      roundtrip(regex(".*".r,  StandardCharsets.UTF_8), "Hello World")
    }
  }

  "zlib" should {
    "roundtrip" in {
      roundtrip(zlib, ByteVector("Hello, World!".getBytes(StandardCharsets.UTF_8)))
    }
  }

  "zeroOrMore" should {
    "roundtrip" in {
      roundtrip(zeroOrMore(ubyte(7)), List(1.toByte, 2.toByte, 3.toByte, 4.toByte))
    }

    "return at first failure" in {
      val codec = zeroOrMore(constant(0xAB.toByte))
      val \/-((remainder, decoded)) = codec.decode(BitVector(Array(0xAB.toByte, 0xAB.toByte, 0xBA.toByte)))
      remainder.length must beEqualTo(8)
      decoded.length must beEqualTo(2)
    }
  }
}
