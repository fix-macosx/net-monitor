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

package babelfish.cms

import java.io.IOException
import java.util

import babelfish.codecs.CodecSpec
import org.bouncycastle.cms.{CMSException, CMSSignedData}
import org.specs2.mutable.Specification

import coop.plausible.nx.assertNonThrows


class CMSTest extends Specification with CodecSpec {
  "signedData" should {
    "roundtrip" in {
      val signedData = assertNonThrows[CMSException] {
        val is = getClass.getResourceAsStream("CMSTestData")
        new CMSSignedData(is)
      }
      roundtrip(CMS.signedData, signedData, { (a:CMSSignedData, b:CMSSignedData) =>
        assertNonThrows[IOException] {
          a.toASN1Structure.getEncoded must beEqualTo(b.toASN1Structure.getEncoded)
        }
      })
    }
  }
}
