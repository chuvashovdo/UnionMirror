package unionmirror.internal.deriver

import scala.deriving.Mirror

import scala.quoted.*

import unionmirror.internal.deriver.{
  DeriverCommon,
  DeriverInstanceSummoning,
  DeriverSamAnalysis,
  DeriverClassCreation,
}

private[unionmirror] object ContravariantSamImpl:
  def contravariantSamImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]]
  )(using
    Quotes
  ): Expr[F[T]] =
    val elems = DeriverCommon.getElems[T]
    val (samName, argName, argTpe, resTpe) = DeriverSamAnalysis.analyzeSam[F, T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)
    DeriverClassCreation.createSamClass[F, T](samName, argName, argTpe, resTpe, instancesExpr, m)
