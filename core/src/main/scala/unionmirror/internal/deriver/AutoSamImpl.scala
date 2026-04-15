package unionmirror.internal.deriver

import scala.deriving.Mirror
import scala.quoted.*

import unionmirror.internal.deriver.{ ContravariantSamImpl, CovariantSamImpl, DeriverSamAnalysis }

object AutoSamImpl:
  def autoSamImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]]
  )(using
    Quotes
  ): Expr[F[T]] =
    import quotes.reflect.*

    val (_, _, argTpe, _) = DeriverSamAnalysis.analyzeSam[F, T]
    val tpe = TypeRepr.of[T]

    if tpe =:= argTpe then ContravariantSamImpl.contravariantSamImpl[F, T](m)
    else CovariantSamImpl.covariantSamImpl[F, T](m)
