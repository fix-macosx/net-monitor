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

package babelfish.indexer.sslsplit

import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Instant

import org.parboiled2.Parser

import scalaz.{-\/, \/-, \/}

/**
 * Protocol and application information available from the SSLsplit log path.
 *
 * @param timestamp The time at which the connection was initiated.
 * @param application The full path of the application that initiated the connection, if known.
 * @param user The name or uid responsible that initiated the connection, if known.
 * @param source The source address of the connection.
 * @param destination The destination address of the connection.
 */
case class SSLSplitLogInfo (timestamp: Instant, application: Option[Path], user: Option[String], source: InetSocketAddress, destination: InetSocketAddress)

object SSLSplitLogInfo {
  /**
   * Parse an SSLsplit log file name, in the format of '%X/%T-%u-%s-%d.log'
   * @param directory The enclosing directory, relative to the root of the log directory; this is used to derive the
   *                  name of the binary corresponding to this log path.
   * @param fileName The log file name.
   */
  def parse (directory: Option[Path], fileName: String): String \/ SSLSplitLogInfo = {
    val parser = new LogPathParser(directory, fileName)
    parser.FileName.run()(Parser.DeliveryScheme.Either) match {
      case Right(info) => \/-(info)
      case Left(error) => -\/("Could not parse log file path: " + parser.formatErrorLine(error))
    }
  }
}