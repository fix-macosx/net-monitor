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

import spire.algebra.{Eq, Order}

/**
 * Represents a finite set of values within a domain.
 *
 * @tparam T The value type.
 */
sealed trait ValueDomain[T] {
  /**
   * Return the values within this domain.
   *
   * Values must be stably ordered according to the domain's ordering rules, if any.
   */
  def orderedValues: List[T]
}

/**
 * A totally ordered finite set of values.
 * @param values The values within this domain.
 * @tparam T The value type.
 */
case class OrderedDomain[T : Order] (values: Set[T]) extends ValueDomain[T] {
  /** The total order of the values */
  val order: Order[T] = implicitly[Order[T]]

  override val orderedValues: List[T] = values.toList.sortWith(order.compare(_, _) <= 0)
}

/**
 * A finite set of unordered values for which equality comparison is supported via the [[Eq]] type class.
 *
 * @param values The values within this domain.
 * @tparam T The value type.
 */
case class EqualityDomain[T : Eq : Hash] (values: EqualitySet[T]) extends ValueDomain[T] {
  val eq: Eq[T] = implicitly[Eq[T]]
  val hash: Hash[T] = implicitly[Hash[T]]

  override val orderedValues: List[T] = values.toList
}

/**
 * A finite set of unordered values, comparable using native Java equality comparison.
 *
 * @param values The values within this domain.
 * @tparam T The value type.
 */
case class IdentityDomain[T] (values: Set[T]) extends ValueDomain[T] {
  override val orderedValues: List[T] = values.toList
}


