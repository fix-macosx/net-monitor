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

package babelfish.indexer

import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Instant

/**
 * Protocol and application information associated with a connection or stateless packet.
 */
trait ConnectionInfo {
  /** The time at which the stateful connection was initiated, or the time at which the packet was sent. */
  def timestamp: Instant

  /** The full path of the application that initiated the connection, if known. */
  def application: Option[Path]

  /** The name or uid responsible that initiated the connection, if known. */
  def user: Option[String]

  /** The source address of the connection. */
  def source: InetSocketAddress

  /** The destination address of the connection. */
  def destination: InetSocketAddress
}