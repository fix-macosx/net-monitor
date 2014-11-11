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

package babelfish.analyzer.solver

import org.specs2.mutable.Specification
import spire.algebra.Eq

class EqualitySetTest extends Specification {
  class ExampleObj (val value: Int)
  implicit object ExHash extends Hash[ExampleObj] { override def hashCode (v: ExampleObj): Int = v.value }
  implicit object ExEq extends Eq[ExampleObj] { override def eqv (x: ExampleObj, y: ExampleObj): Boolean = x.value == y.value }

  "equality sets" should {
    "handle contains()" in {
      val eqSet = EqualitySet(new ExampleObj(1), new ExampleObj(2))
      eqSet.contains(new ExampleObj(1)) must beTrue
    }

    "exclude duplicate values" in {
      val set = Set(new ExampleObj(1), new ExampleObj(2), new ExampleObj(1))
      set.size must beEqualTo(3)

      val eqSet = EqualitySet(set.toSeq:_*)
      eqSet.size must beEqualTo(2)
      eqSet == EqualitySet(new ExampleObj(1), new ExampleObj(2)) must beTrue
    }
  }
}
