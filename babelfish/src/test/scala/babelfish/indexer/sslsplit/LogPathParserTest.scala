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

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.file.{Paths, Path}
import java.time.Instant

import org.parboiled2.Parser
import org.specs2.mutable.Specification


class LogPathParserTest extends Specification {
  /**
   * Parse the given input, returning either the parsed document, or a formatted parser error.
   *
   * @param input The input to parse.
   */
  def parse (directory: Option[Path], input: String): Either[String, SSLSplitLogInfo] = {
    val parser = new LogPathParser(directory, input)

    parser.FileName.run()(Parser.DeliveryScheme.Either) fold (
      failure => Left(parser.formatError(failure, showTraces = true)),
      success => Right(success)
    )
  }

  val fileName = "20141019T193303Z-effuser-[172.16.174.146]:49689-[23.67.252.215]:80.log"

  "file name parsing" should {
    "reject files that do not end with '.log'" in {
      parse(None, fileName.take(fileName.lastIndexOf(".log"))) must beLeft
    }
  }

  "date parsing" should {
    "parse dates" in {
      parse(None, fileName).right.map(_.timestamp) must beRight(===(Instant.parse("2014-10-19T19:33:03Z")))
    }
  }

  "user parsing" should {
    "parse users" in {
      parse(None, fileName).right.toOption.flatMap(_.user) must beSome(===("effuser"))
    }

    "handle missing users" in {
      val fileName = "20141019T193303Z--[172.16.174.146]:49689-[23.67.252.215]:80.log"
      parse(None, fileName).right.toOption.flatMap(_.user) must beNone
    }
  }

  "ipv4 address parsing" should {
    "parse ipv4 source address" in {
      val saddr:SocketAddress = new InetSocketAddress("172.16.174.146", 49689)
      parse(None, fileName).right.map(_.source) must beRight(===(saddr))
    }

    "parse ipv4 destination address" in {
      val saddr:SocketAddress = new InetSocketAddress("23.67.252.215", 80)
      parse(None, fileName).right.map(_.destination) must beRight(===(saddr))
    }
  }

  "ipv6 address parsing" should {
    def populateAddress (src: String, dest: String): String = s"20141019T193303Z--[$src]:49689-[$dest]:80.log"

    "parse ipv6 source address" in {
      val addr = "2001:db8:85a3:8d3:1319:8a2e:370:7348"
      val saddr:SocketAddress = new InetSocketAddress(addr, 49689)
      parse(None, populateAddress(addr, "::1")).right.map(_.source) must beRight(===(saddr))
    }

    "parse ipv6 destination address" in {
      val addr = "2001:db8:85a3:8d3:1319:8a2e:370:7348"
      val saddr:SocketAddress = new InetSocketAddress(addr, 80)
      parse(None, populateAddress("::1", addr)).right.map(_.destination) must beRight(===(saddr))
    }

    "parse :: and ::1 addresses" in {
      parse(None, populateAddress("::", "::1")).right.map(_.destination.getAddress.isLoopbackAddress) must beRight(===(true))

      parse(None, populateAddress("::1", "::")).right.map(_.source.getAddress.isLoopbackAddress) must beRight(===(true))
    }
  }

  "application path parsing" should {
    "parse application paths" in {
      val path = Paths.get("best", "app")
      parse(Some(path), fileName).right.toOption.flatMap(_.application) must beSome(path)
    }

    "handle missing application paths" in {
      parse(None, fileName).right.toOption.flatMap(_.application) must beNone
    }

    "treat root paths as missing" in {
      parse(Some(Paths.get("///")), fileName).right.toOption.flatMap(_.application) must beNone
    }
  }
}
