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

import scala.annotation.tailrec
import scalaz.\/
import scalaz._, Scalaz._

/**
 * HTTP content information (content length, transfer encoding).
 */
sealed trait ContentInfo

/**
 * The body of this request (or response) is not chunked.
 *
 * @param length The total content length.
 */
case class ContentLength (length: Int) extends ContentInfo

/**
 * The body of this request is chunked.
 */
case object ContentChunked extends ContentInfo

/**
 * No content information was supplied in the HTTP headers.
 */
case object ContentUnknown extends ContentInfo

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

  /** Return the content info; this is based on the Content-Length and Transfer-Encoding headers */
  def contentInfo: String \/ ContentInfo = {
    val handlers = List[(String, String => String \/ ContentInfo)] (
      ("transfer-encoding", (value: String) => value.toLowerCase(Locale.US) match {
        case "chunked" => \/-(ContentChunked)
        case _ => -\/(s"Unknown Transfer-Encoding `$value`")
      }),

      ("content-length", (value:String) => try {
        \/-(ContentLength(value.toInt))
      } catch {
        case nfe:NumberFormatException => -\/(s"`Content-Length' value is not an integer: $value")
      })
    )

    @tailrec def loop (handlers: List[(String, String => String \/ ContentInfo)]): String \/ ContentInfo = handlers match {
      case (headerName, f) :: tail =>
        val result = headerTable.get(headerName).flatMap(_.headOption).toRightDisjunction(s"$headerName not found").flatMap(f)
        if (result.isLeft) {
          loop(tail)
        } else {
          result
        }

      case Nil => \/-(ContentUnknown)
    }

    loop(handlers)
  }
}