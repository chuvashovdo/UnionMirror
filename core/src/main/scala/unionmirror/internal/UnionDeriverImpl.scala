package unionmirror.internal

import scala.deriving.Mirror
import scala.quoted.*

import unionmirror.UnionDeriver
import unionmirror.internal.deriver.{ ContravariantWithBuilderImpl, ContravariantSamImpl }

private[unionmirror] object UnionDeriverImpl:
  inline def deriveContravariantWithBuilder[F[_], T](
    using
    m: Mirror.SumOf[T],
    b: UnionDeriver.ContravariantInstanceBuilder[F],
  ): F[T] =
    ${ ContravariantWithBuilderImpl.contravariantWithBuilderImpl[F, T]('m, 'b) }

  inline def deriveContravariantSAM[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    ${ ContravariantSamImpl.contravariantSamImpl[F, T]('m) }
