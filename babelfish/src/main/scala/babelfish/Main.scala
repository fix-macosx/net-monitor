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

package babelfish

import java.io._

import argonaut.Argonaut._
import argonaut._
import babelfish.codecs.http.HTTP
import scodec.bits._
import scodec.codecs.utf8

import scalaz.Scalaz._
import scalaz._

object Main extends App {
  /* Read input */
  val input = args.headOption.toRight("Usage: decode <path>").right.flatMap { inputFile =>
    try {
      val inputStream = new FileInputStream(new File(inputFile))
      Right(BitVector.fromInputStream(inputStream))
    } catch {
      case ioe: IOException =>
        Left(s"Could not read ${args(0)}: ${ioe.getMessage}")
    }
  }

  val result = input.right.flatMap { bits =>
    val result = HTTP.http.complete.decode(bits)
    result.toEither
  }

  result.left.foreach(msg => println(s"Parse failed: $msg"))
  result.right.foreach { rl =>
    val (_, log) = rl
    println(s"Parsed $log")
    log.response.body.toArray

    import babelfish.codecs._
    import babelfish.codecs.CMSSignedDataCodec.syntax._


    val data = for (
      signedData <- cms.signedData.complete.decode(log.response.body.toBitVector).flatMap(_._2.signedBytes);
      decoded <- base64(Base64Codec.MIME).complete.decode(BitVector(signedData));
      decompressed <- zlib.decode(decoded._2.toBitVector);
      jsonString <- utf8.decode(decompressed._2.toBitVector);
      json <- jsonString._2.parse
    ) yield json
    data.foreach(d => println(s"Parsed:\n${d.spaces2}"))

  }
}
