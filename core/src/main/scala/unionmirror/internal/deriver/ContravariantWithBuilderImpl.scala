package unionmirror.internal.deriver

import scala.deriving.Mirror

import scala.quoted.*

import unionmirror.UnionDeriver

import unionmirror.internal.deriver.{ DeriverCommon, DeriverInstanceSummoning }

object ContravariantWithBuilderImpl:
  def contravariantWithBuilderImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]],
    b: Expr[UnionDeriver.ContravariantInstanceBuilder[F]],
  )(using
    Quotes
  ): Expr[F[T]] =
    val elems = DeriverCommon.getElems[T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)
    '{
      val instances = $instancesExpr
      val dispatch: T => F[Any] =
        (t: T) =>
          val ord = $m.ordinal(t)
          instances(ord).asInstanceOf[F[Any]]
      $b.build[T](dispatch)
    }
