package babelfish.common

import java.nio.charset.StandardCharsets

import org.specs2.mutable.Specification
import scodec.bits.ByteVector

class CompressionTest extends Specification with CodecSpec {
  "zlib" should {
    "roundtrip" in {
      roundtrip(Compression.zlib, ByteVector("Hello, World!".getBytes(StandardCharsets.UTF_8)))
    }
  }
}
