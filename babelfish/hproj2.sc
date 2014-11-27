import babelfish.analyzer.solver.{Var, Macros}
import shapeless._
import shapeless.syntax._

class ClassProjection[T] {
  import scala.language.experimental.macros

  def apply[U] (expr: T => U): Var[U] = macro Macros.var_impl[T, U]
  def vars: AnyRef = macro Macros.vars_impl[T]
}

sealed trait Domain[A]
case class SetDomain[A] (values: Set[A]) extends Domain[A]
case class Literal[A] (value: A) extends Domain[A]

sealed trait Constraint[A] extends Domain[A] {
  def as[CaseClassType] (implicit projector: ConstraintProjector[A, CaseClassType]) = projector(this)
}

object Constraint {
  implicit class HListConstraintOperations[L <: HList] (val self: Constraint[L]) /* TODO extends AnyVal */ {
    def ::[B] (rhs: Constraint[B]): Constraint[B :: L] = new Constraint[B :: L] {
      // TODO
    }
  }
  implicit class ValueConstraintOperations[A] (val self: Constraint[A]) /* TODO extends AnyVal */ {
    def ::[B] (rhs: Constraint[B]): Constraint[B :: A :: HNil] = new Constraint[B :: A :: HNil] {
      // TODO
    }
  }
}

case class Eq[C, T] (domain: Domain[T]) extends Constraint[T]

trait ConstraintProjector[A, B] {
  def apply (ca: Constraint[A]): ClassProjection[B]
}

object ConstraintProjector {
  implicit def makeProjector[Elems, Repr, Proj] (implicit gen: Generic.Aux[Proj, Repr], listToProjection: Elems =:= Repr, projectionToList: Repr =:= Elems): ConstraintProjector[Elems, Proj]  = new ConstraintProjector[Elems, Proj] {
    def apply (ca: Constraint[Elems]): ClassProjection[Proj] = {
      val from: Elems => Proj = a => gen.from(a)
      val to: Proj => Elems = b => gen.to(b)
      new ClassProjection[Proj]()
    }
  }
}

case class Name (first: String, last: String)
val l = Literal("A Name")
val eq = Eq(SetDomain(Set("Foo", "Bar")))
val eq2 = Eq(eq)
val c = (eq :: eq2).as[Name]

c(_.first)
