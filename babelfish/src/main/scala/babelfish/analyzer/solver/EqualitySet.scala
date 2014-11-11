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

import spire.algebra.Eq

import scala.collection.SetLike

/**
 * An implementation of Scala's immutable Set in which object equality and hash codes are computed
 * using the [[Hash]] and [[Eq]] type classes.
 *
 * @param backing The backing store for this equality set.
 * @tparam T Element type.
 */
class EqualitySet[T : Eq : Hash] private (backing: Set[EqualitySet.EqElem[T]]) extends Set[T] {
  import EqualitySet.EqElem

  override def contains (elem: T)   = backing.contains(new EqElem(elem))
  override def + (elem: T)          = new EqualitySet(backing + new EqElem(elem))
  override def - (elem: T)          = new EqualitySet(backing - new EqElem(elem))
  override def iterator             = backing.iterator.map(_.elem)
  override def empty                = new EqualitySet[T](Set.empty)
}

/**
 * Companion object.
 */
object EqualitySet {
  /**
   * An element wrapper that supports the storage of elements in standard
   * Scala collections while using the [[Eq]] and [[Hash]] type classes
   * to provide equality and hashCode support.
   *
   * @param elem The actual value.
   * @tparam T The value type.
   */
  private class EqElem[T : Eq : Hash] (val elem: T) {
    private val eq = implicitly[Eq[T]]
    private val hash = implicitly[Hash[T]]

    override def hashCode (): Int = hash.hashCode(elem)
    override def equals (obj: scala.Any): Boolean = obj match {
      case other:EqElem[_] if elem.getClass.isAssignableFrom(other.elem.getClass) =>
        eq.eqv(elem, other.elem.asInstanceOf[T])
      case _ => false
    }
  }

  /**
   * Create a new [[EqualitySet]].
   *
   * @param elems The elements to be included in the new set.
   * @tparam T The element type.
   * @return A new equality set.
   */
  def apply[T : Eq : Hash] (elems: T*): EqualitySet[T] = {
    new EqualitySet(elems.map(new EqElem(_)).toSet)
  }
}
