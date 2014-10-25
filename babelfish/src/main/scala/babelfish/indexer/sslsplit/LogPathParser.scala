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

import java.math.BigInteger
import java.net._
import java.nio.file.{Paths, Path}
import java.time.Instant

import org.parboiled2.CharPredicate._
import org.parboiled2._

import coop.plausible.nx.assertNonThrows

/**
 * Parsing of Net-Monitor SSLsplit file names in the format of '%X/%T-%u-%s-%d.log'
 *
 * @param directory The enclosing directory, relative to the root of the log directory; this is used to derive the
 *                  name of the binary corresponding to this log path.
 * @param fileName The log file name.
 */
private[sslsplit] class LogPathParser (val directory: Option[Path], val fileName: String) extends Parser {
  override def input: ParserInput = fileName

  /* Extract the application path, rejecting empty root paths */
  private val applicationPath = directory.flatMap { path =>
    if (path.getNameCount == 0 || path == Paths.get("/")) {
      None
    } else {
      Some(path)
    }
  }

  /**
   * Sequence of `count` digits.
   * @param count Number of digits to capture.
   */
  private def DigitSequence (count: Int): Rule1[String] = rule { capture(count.times(Digit)) }

  /** ISO 8601 Timestamp -- no '-/:' separators */
  private def TimeStamp = rule {
    DigitSequence(4) ~ DigitSequence(2) ~ DigitSequence(2) ~ 'T' ~ DigitSequence(2) ~ DigitSequence(2) ~ DigitSequence(2) ~ 'Z' ~> { (yr, month, day, hr, mn, s) =>
      Instant.parse(s"$yr-$month-${day}T$hr:$mn:${s}Z")
    }
  }

  /** Username field; accepts any character except the leading text of the next field ('-[') */
  private def Username = rule { capture(oneOrMore(&(!"-[") ~ ANY)) }

  /** TCP or UDP port number */
  private def Port: Rule1[Int] = rule { capture((1 to 5).times(Digit)) ~> { (strval:String) =>
    /* Port must be <= UINT16_MAX */
    val bigval = BigInt(strval)
    test(bigval <= 65535) ~ push(bigval.intValue())
  }}

  /** Single octet of IPv4 address */
  private def IPv4Octet = rule { (1 to 3).times(Digit) }

  /** IPv4 address string */
  private def IPv4 = rule { capture(4.times(IPv4Octet).separatedBy(".")) ~>
    { (str:String) =>
      assertNonThrows[UnknownHostException](InetAddress.getByName(str))
    }
  }

  /** Single word of IPv6 address */
  private def IPv6Word = rule { (0 to 4).times(HexDigit) }

  /** IPv6 address string */
  private def IPv6 = rule { capture((2 to 16).times(IPv6Word).separatedBy(":")) ~> { (str:String) =>
      assertNonThrows[UnknownHostException](InetAddress.getByName(str))
    }
  }

  /** Host address and port */
  private def SockAddr: Rule1[InetSocketAddress] = rule { "[" ~ (IPv4 | IPv6) ~ "]:" ~ Port ~> { (addr: InetAddress, port: Int) =>
      new InetSocketAddress(addr, port)
    }
  }


  /** Performs parsing of the log path's relative file name */
  def FileName: Rule1[SSLSplitLogInfo] = rule {
    TimeStamp ~ '-' ~
    optional(Username) ~ "-" ~
    SockAddr ~ "-" ~
    SockAddr ~ ".log" ~> { (ts: Instant, username: Option[String], source: InetSocketAddress, dest: InetSocketAddress) =>
      SSLSplitLogInfo(ts, applicationPath, username, source, dest)
    }
  }

}
