package babelfish.cms

import java.io.{ByteArrayOutputStream, IOException}

import org.bouncycastle.cms.{CMSException, CMSSignedData}
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scalaz.{-\/, \/-, \/}

import coop.plausible.nx.assertNonThrows

/**
 * Cryptographic Message Syntax Codecs
 */
object CMS {

  /**
   * Codec-related API extensions for CMSSignedData
   * @param value The wrapped CMSSignedData value.
   */
  implicit class CMSSignedDataCodecExtensions (val value: CMSSignedData) extends AnyVal {
    /**
     * Return the signed content bytes from this message.
     */
    def signedBytes: Array[Byte] = {
      val bo = new ByteArrayOutputStream()
      assertNonThrows[IOException](value.getSignedContent.write(bo))
      assertNonThrows[Exception](bo.close())
      bo.toByteArray
    }
  }


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

    /**
     * An implementation of the signedData codec that operates directly on ByteVector values.
     */
    val bytes: Codec[ByteVector] = this.exmap (x => \/-(ByteVector(x.signedBytes)), { (x:ByteVector) =>
      try {
        \/-(new CMSSignedData(x.toArray))
      } catch {
        case ex:Exception => -\/(s"Could not parse bytes as CMS signed data: ${ex.getMessage}")
      }
    })
  }
}

