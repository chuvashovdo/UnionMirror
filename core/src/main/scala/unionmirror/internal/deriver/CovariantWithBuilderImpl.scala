package unionmirror.internal.deriver

import scala.annotation.unused
import scala.deriving.Mirror

import scala.quoted.*

import unionmirror.UnionDeriver

import unionmirror.internal.deriver.{ DeriverCommon, DeriverInstanceSummoning }

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.AsInstanceOf"))
private[unionmirror] object CovariantWithBuilderImpl:
  def covariantWithBuilderImpl[F[_]: Type, T: Type](
    @unused _m: Expr[Mirror.SumOf[T]],
    b: Expr[UnionDeriver.CovariantInstanceBuilder[F]],
  )(using
    Quotes
  ): Expr[F[T]] =
    val elems = DeriverCommon.getElems[T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)

    '{
      val instances = $instancesExpr
      val elemsSeq: IndexedSeq[F[Any]] =
        scala.collection.immutable.ArraySeq.unsafeWrapArray(instances).map(_.asInstanceOf[F[Any]])
      $b.build[T](elemsSeq)
    }
