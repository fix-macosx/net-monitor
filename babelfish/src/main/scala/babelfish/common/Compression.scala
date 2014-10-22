package babelfish.common

import java.io.{IOException, ByteArrayOutputStream}
import java.util.zip.{DataFormatException, Inflater, Deflater}

import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scalaz.{-\/, \/-, \/}

import coop.plausible.nx.assertNonThrows

/**
 * Compression codecs
 */
object Compression {
  /**
   * ZLIB compression & decompression.
   */
  object zlib extends Codec[ByteVector] {
    /** @inheritdoc */
    override def decode (bits: BitVector): \/[String, (BitVector, ByteVector)] = try {
      val inflater = new Inflater()
      val inputBytes = bits.toByteArray
      inflater.setInput(inputBytes)

      val output = new ByteArrayOutputStream(inputBytes.length)
      val buffer = new Array[Byte](inputBytes.length.min(1024))
      while (!inflater.finished()) {
        val n = inflater.inflate(buffer)
        assertNonThrows[IOException](output.write(buffer, 0, n))
      }

      assertNonThrows[Exception](output.close())

      \/-((BitVector.empty, ByteVector(output.toByteArray)))
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
}
