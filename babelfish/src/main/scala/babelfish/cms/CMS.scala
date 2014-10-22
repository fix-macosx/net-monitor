package babelfish.cms

import java.io.IOException

import org.bouncycastle.cms.{CMSException, CMSSignedData}
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scalaz.{-\/, \/-, \/}

/**
 * Cryptographic Message Syntax Codecs
 */
object CMS {
  /**
   * Parsing of CMS signed data blocks.
   */
  object signedData extends Codec[CMSSignedData] {
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
}

