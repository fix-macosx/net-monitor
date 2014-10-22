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

import babelfish.http.HTTP
import scodec.bits._

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
  result.right.foreach(log => println(s"Parsed $log"))
}
