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

import spire.math.Numeric

/**
 * A type class representing elements for which a hash code may be computed.
 *
 * @tparam T The supported type.
 */
trait Hash[T] {
  /**
   * Return a stable hash code for the given value.
   *
   * @param v The value for which a hash code should be returned.
   * @return A stable hash code.
   */
  def hashCode (v: T): Int
}

object Hash {
  /**
   * Returns an implementation of [[Hash]] for use with numeric values.
   * @tparam T The numeric type.
   */
  implicit def numericHash[T : Numeric]: Hash[T] = new Hash[T] {
    override def hashCode (v: T): Int = v.hashCode()
  }
}
