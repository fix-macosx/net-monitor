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

package babelfish.codecs.http

import java.util.Locale


/**
 * A set of HTTP headers.
 *
 * @param headers HTTP headers, in encoding/decoding order.
 */
case class Headers (headers: List[Header]) {
  /** Map of actual header values */
  private lazy val headerTable: Map[String, List[String]] = headers.foldLeft(Map.empty[String, List[String]]) { (m, next) =>
    val values = m.getOrElse(next.name.toLowerCase(Locale.US), List.empty) :+ next.value
    m + ((next.name.toLowerCase(Locale.US), values))
  }

  /** Return the content length, if known */
  def contentLength: Either[String, Int] = {
    val stringVal = headerTable.get("content-length").map(_.headOption).flatten
    try {
      stringVal.map(_.toInt).toRight("Missing `Content-Length' header")
    } catch {
      case nfe:NumberFormatException => Left(s"`Content-Length' value is not an integer: $stringVal")
    }
  }
}