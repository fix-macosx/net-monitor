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

// TODO: Replace with Variable
case class Var[T] ()

/**
 * Private macro implementations.
 */
object Macros {
  import scala.reflect.macros.{whitebox, blackbox}

  /**
   * Private macro implementation; returns a [[Variable]] instance
   * for a case class field identified by the invocation of its accessor
   * in `expr`.
   * 
   * @param c The macro context.
   * @param expr An expression of the form `(c:C) => c.accessor`.
   * @tparam C The case class type on which the target field is defined.
   * @tparam V The case class field type for which the [[Variable]] instance will be created.
   */
  def var_impl[C : c.WeakTypeTag, V : c.WeakTypeTag] (c: blackbox.Context) (expr: c.Expr[C => V]) = {
    import c.universe.{Literal => Lit, _}

    /* Attempt to formulate our Var() invocation */
    val createVar = for (
      /* Validate the function expression and extract the field reference */
      fieldInfo <- {
          expr.tree match {
            case Function(List(ValDef(mods, TermName(_), TypeTree(), EmptyTree)), select@Select(_, ident)) if mods.hasFlag(Flag.PARAM) => Right((select.pos, ident))
            case other =>
              println(showRaw(expr.tree))
              c.error(expr.tree.pos, s"Expression must be a function of form `(v:${weakTypeOf[C].typeSymbol.name}) => v.field`")
              Left(())
          }
        }.right;

        fieldPos <- Right(fieldInfo._1).right;
        fieldIdent <- Right(fieldInfo._2).right;

        /* Match to an actual field */
        fieldMethod <- weakTypeOf[C].decls.collect {
          case m: MethodSymbol if m.isCaseAccessor && m.name == fieldIdent => Right(m)
        }.headOption.getOrElse {
          /* This shouldn't be possible if the expression has already type checked and passed our
           * match clause above. */
          c.error(fieldPos, s"`${weakTypeOf[C].typeSymbol.name}.${fieldIdent.toString}` does not exist")
          Left(())
        }.right
    ) yield Apply(
      TypeApply(
        Select(Ident(weakTypeOf[Var[_]].typeSymbol.companion), TermName("apply")),
        List(Ident(fieldMethod.returnType.typeSymbol))
      ),
      List()
    )

    val tree = createVar match {
      case Left(_) => Lit(Constant(null))
      case Right(application) => application
    }

    c.Expr[Var[V]](tree)
  }

  /**
   * Private macro implementation; returns a structural type vending [[Variable]] instances
   * for all case class fields defined on the given type `T`.
   *
   * @param c The macro context.
   * @tparam T The type for which all case class attributes will be returned.
   */
  def vars_impl[T : c.WeakTypeTag] (c: whitebox.Context) = {
    import c.universe.{Literal => Lit, _}

    /* Fetch all case class accessors for the target type, and generate our own set of
     * Variable accessor methods */
    val accessors = weakTypeOf[T].decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.map { accessor =>
      DefDef(
        Modifiers(),
        accessor.name,
        Nil,
        Nil,
        TypeTree(),
        Apply(
          TypeApply(
            Select(Ident(weakTypeOf[Var[_]].typeSymbol.companion), TermName("apply")),
            List(Ident(accessor.returnType.typeSymbol))
          ),
          List()
        )
      )
    }.toList

    /* Define our structural class */
    val varProjector = ClassDef(
      Modifiers(),
      TypeName(c.freshName()),
      Nil,
      Template(
        Nil,
        noSelfType,
        /* The class body; comprised of our constructor and set of generated accessors */
        DefDef( /* Constructor */
          Modifiers(),
          termNames.CONSTRUCTOR,
          Nil,
          Nil :: Nil,
          TypeTree(),
          Block(
            List[Tree](
              Apply(
                Select(Super(This(typeNames.EMPTY), typeNames.EMPTY), termNames.CONSTRUCTOR),
                Nil
              )
            ),
            Lit(Constant(()))
          )
        ) +: accessors
      )
    )

    /* Return an instance of our projecting class. */
    c.Expr(
      Block(
        List(varProjector),
        Apply(Select(New(Ident(varProjector.name)), termNames.CONSTRUCTOR), Nil)
      )
    )
  }
}