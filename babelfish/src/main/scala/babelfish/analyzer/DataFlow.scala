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

package babelfish.analyzer

import java.nio.charset.StandardCharsets

trait DataFlow {
  import scala.language.implicitConversions

  def subtype[T <: Category#Subtype] (typed: T): Rule[T] = ???

  def not[T] (negated: Rule[T]): Rule[Unit] = ???

  implicit def category[F <: Category] (cat: F): Rule[F] = ???

  implicit class TypeParamSyntax[P <: Param[_]] (val self: P) {
    def apply[V <: self.type#Value] (rhs: => V): Rule[V] = isEqual(rhs)
    def isEqual[V <: self.type#Value] (rhs: => V): Rule[V] = ???
  }
}

object TestDataFlow extends DataFlow {
  import Category._


  val csp = Text :: (
    Param.Charset(StandardCharsets.US_ASCII) or
      Param.Charset(StandardCharsets.UTF_8)
    )
  csp.doSomething { value =>
    val t:Text.type = value(0)
  }
}