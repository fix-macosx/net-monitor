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
import shapeless._

import scala.annotation.implicitNotFound

/**
 * A finite set of values against which a [[Constraint]] may be applied.
 * @tparam A The value type represented by this domain.
 */
sealed trait Domain[A]

/**
 * A finite set of unordered values, comparable using native Scala equality comparison.
 *
 * @param values The values within this domain.
 * @tparam A The value type represented by this domain.
 */
case class IdentityDomain[A] (values: Set[A]) extends Domain[A]

/**
 * A domain comprised of a single value.
 *
 * @param value The value defined by this domain.
 * @tparam A The value type.
 */
case class Literal[A] (value: A) extends Domain[A]

/**
 * A solver constraint over values of type [[T]].
 *
 * @tparam T The type of values over which this constraint is defined.
 */
sealed trait Constraint[T] extends Domain[T] {
  import scala.language.experimental.macros

  /**
   * Return a new [[Constraint]] of type [[U]] based on an isomorphism between constraints of type
   * [[T]] and [[U]].
   */
  def as[U] (implicit map: ConstraintIsomorphism[T, U]): Constraint[U] = map(this)
  
  /**
   * Return the [[Constraint]] instance corresponding to a field accessor on
   * type [[T]] accessed in `expr`.
   *
   * If type [[T]] is not a case class, or `expr` references an invalid case class
   * field, compilation will fail.
   *
   * @param expr An expression of the form `(c:T) => c.field`.
   * @tparam U The field type.
   */
  def field[U] (expr: T => U): Variable[U] = macro Macros.var_impl[T, U]

  /**
   * EXPERIMENTAL: Return a structural type vending [[Variable]] field accessors
   * corresponding to all case class accessors defined on [[T]].
   */
  private[solver] def fields: AnyRef = macro Macros.vars_impl[T]
}

/**
 * Supported constraint types.
 */
object Constraint {

  /**
   * An equality constraint.
   *
   * @param domain The domain within which a value must be defined to satisfy this constraint.
   * @tparam T The value type over which this constraint operates.
   */
  case class Eq[T] (domain: Domain[T]) extends Constraint[T]

  /**
   * Constraint operations applicable to [[Constraint]]s over HList values.
   * @param self The wrapped [[Constraint]] instance.
   * @tparam L The HList type of the wrapped constraint.
   */
  implicit class HListConstraintOperations[L <: HList] (val self: Constraint[L]) extends AnyVal {
    /**
     * Return a new aggregate [[Constraint]] over `B :: L :: HNil`
     * @param rhs The additional constraint to be included in the returned aggregate constraint.
     *
     * @tparam B The `rhs` constraint type.
     */
    def ::[B] (rhs: Constraint[B]): Constraint[B :: L] = new Constraint[B :: L] {
      // TODO
    }
  }

  /**
   * Constraint operations applicable to [[Constraint]]s over non-HList values.
   *
   * @param self The wrapped [[Constraint]] instance.
   * @tparam A The type of the wrapped constraint.
   */
  implicit class ValueConstraintOperations[A] (val self: Constraint[A]) extends AnyVal {
    /**
     * Return a new aggregate [[Constraint]] over `B :: L :: HNil`
     * @param rhs The additional constraint to be included in the returned aggregate constraint.
     *
     * @tparam B The `rhs` constraint type.
     */
    def ::[B] (rhs: Constraint[B]): Constraint[B :: A :: HNil] = new Constraint[B :: A :: HNil] {
      // TODO
    }
  }
}

/**
 * An effectively bidirectional mapping (i.e. an isomorphism) between [[Constraint]] instances
 * with compatible shapes; for example, between an HList of type `String :: Int :: HNil` and
 * a case class with a constructor of type `String, Int`
 *
 * @tparam L The value type of the [[Constraint]] to which this type class instance applies.
 * @tparam T The case class type to which constraints of type [[L]] will be mapped.
 *
 * ''Credit: This design is based on Michael Pilquist's [https://github.com/scodec/scodec scodec] case class mapping.''
 */
@implicitNotFound("""Could not find an instance of the CaseClassConstraintMapper type class providing conversion to/from ${L} and ${T}. A type class instance is automatically provided for case classes and HLists of the same shape.""")
trait ConstraintIsomorphism[L, T] {
  /**
   * Return a constraint of type [[T]] for the given constraint of type [[L]].
   *
   * @param ca A constraint for which an isomorphism exists with a constrain of type [[T]].
   */
  def apply (ca: Constraint[L]): Constraint[T]
}

/**
 * Default [[ConstraintIsomorphism]] implicits.
 */
object ConstraintIsomorphism {
  import scala.language.implicitConversions

  /**
   * Given a [[Constraint]] over type [[L]] and an isomorphism between [[L]] and [[T]], return
   * the corresponding [[ConstraintIsomorphism]] from [[L]] to [[T]].
   *
   * This provides automatic mapping of `HList` constraints to compatible case classes.
   *
   * @param gen A Shapeless Generic that may be used to map to and from [[L]]/[[T]].
   * @param lToR Proof that type [[L]] is equal to type [[Repr]].
   * @param rToL Proof that type [[Repr]] is equal to type [[L]]
   *
   * @tparam L A type for which a Shapeless Generic isomorphism exists.
   * @tparam T The case class type for which a Shapeless Generic isomorphism exists.
   * @tparam Repr A type equivalent to [[L]] over which the generic isomorphism is defined.
   */
  implicit def constraintAs[L, Repr, T] (implicit gen: Generic.Aux[L, Repr], lToR: L =:= Repr, rToL: Repr =:= L): ConstraintIsomorphism[L, T]  = new ConstraintIsomorphism[L, T] {
    override def apply (ca: Constraint[L]): Constraint[T] = {
      // TODO - We need to use `gen`'s `from` and `to` functions to actually create our case class instances.
      new Constraint[T]() { }
    }
  }
}