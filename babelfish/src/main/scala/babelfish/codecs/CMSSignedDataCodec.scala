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

import java.io.{ByteArrayOutputStream, IOException}

import org.bouncycastle.cms.{CMSException, CMSSignedData}
import scodec.Codec
import scodec.bits.BitVector

import scalaz.{-\/, \/, \/-}

/**
 * Parsing of CMS signed data blocks.
 */
case class CMSSignedDataCodec () extends Codec[CMSSignedData] {
  override def decode (bits: BitVector): \/[String, (BitVector, CMSSignedData)] = try {
    \/-((BitVector.empty, new CMSSignedData(bits.toByteArray)))
  } catch {
    case ce:CMSException => -\/(s"Could not decode as CMS: ${ce.getMessage}")
  }

  override def encode (value: CMSSignedData): \/[String, BitVector] = try {
    \/-(BitVector(value.toASN1Structure.getEncoded))
  } catch {
    case ioe:IOException => -\/(s"Could not encode as CMS: ${ioe.getMessage}")
  }
}

/**
 * CMSSignedDataCodec Syntax Extensions
 */
object CMSSignedDataCodec {
  /**
   * CMSSignedDataCodec and CMSSignedData syntax extensions.
   */
  object syntax {
    /**
     * Codec-related API extensions for CMSSignedData
     *
     * @param value The wrapped CMSSignedData value.
     */
    implicit class CMSSignedDataCodecExtensions (val value: CMSSignedData) extends AnyVal {
      /**
       * Return the signed content bytes from this message.
       */
      def signedBytes: String \/ Array[Byte] = {
        val bo = new ByteArrayOutputStream()
        try {
          value.getSignedContent.write(bo)
          bo.close()
          \/-(bo.toByteArray)
        } catch {
          case e:Exception => -\/("Could not decode CMS signed data")
        }
      }
    }
  }

}