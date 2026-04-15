package unionmirror.internal

import scala.deriving.Mirror
import scala.quoted.*

import unionmirror.UnionDeriver
import unionmirror.internal.deriver.{
  ContravariantWithBuilderImpl,
  ContravariantSamImpl,
  CovariantWithBuilderImpl,
  CovariantSamImpl,
  BinaryWithBuilderImpl,
  AutoSamImpl,
}

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

  inline def deriveCovariantWithBuilder[F[_], T](
    using
    m: Mirror.SumOf[T],
    b: UnionDeriver.CovariantInstanceBuilder[F],
  ): F[T] =
    ${ CovariantWithBuilderImpl.covariantWithBuilderImpl[F, T]('m, 'b) }

  inline def deriveCovariantSAM[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    ${ CovariantSamImpl.covariantSamImpl[F, T]('m) }

  inline def deriveBinaryWithBuilder[F[_], T](
    using
    m: Mirror.SumOf[T],
    b: UnionDeriver.BinaryInstanceBuilder[F],
  ): F[T] =
    ${ BinaryWithBuilderImpl.binaryWithBuilderImpl[F, T]('m, 'b) }

  inline def deriveAutoSAM[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    ${ AutoSamImpl.autoSamImpl[F, T]('m) }
