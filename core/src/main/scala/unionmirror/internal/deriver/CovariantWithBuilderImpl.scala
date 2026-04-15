package unionmirror.internal.deriver

import scala.deriving.Mirror

import scala.quoted.*

import unionmirror.UnionDeriver

import unionmirror.internal.deriver.{ DeriverCommon, DeriverInstanceSummoning }

object CovariantWithBuilderImpl:
  def covariantWithBuilderImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]],
    b: Expr[UnionDeriver.CovariantInstanceBuilder[F]],
  )(using
    Quotes
  ): Expr[F[T]] =
    val elems = DeriverCommon.getElems[T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)

    '{
      val instances = $instancesExpr
      val elemsList: List[F[Any]] = instances.map(_.asInstanceOf[F[Any]]).toList
      $b.build[T](elemsList)
    }
