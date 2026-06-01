package unionmirror

import scala.compiletime
import scala.compiletime.summonFrom
import scala.deriving.Mirror

import unionmirror.internal.UnionDeriverImpl

object UnionDeriver:
  trait ContravariantInstanceBuilder[F[_]]:
    def build[T](dispatch: T => F[Any]): F[T]

  trait CovariantInstanceBuilder[F[_]]:
    def build[T](elems: IndexedSeq[F[Any]]): F[T]

  trait BinaryInstanceBuilder[F[_]]:
    def build[T](ordinal: T => Int, elems: IndexedSeq[F[Any]]): F[T]

  inline def deriveContravariant[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    summonFrom {
      case b: ContravariantInstanceBuilder[F] =>
        UnionDeriverImpl.deriveContravariantWithBuilder[F, T](using m, b)
      case _ =>
        UnionDeriverImpl.deriveContravariantSAM[F, T](using m)
    }

  inline def deriveCovariant[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    summonFrom {
      case b: CovariantInstanceBuilder[F] =>
        UnionDeriverImpl.deriveCovariantWithBuilder[F, T](using m, b)
      case _ =>
        UnionDeriverImpl.deriveCovariantSAM[F, T](using m)
    }

  inline def deriveBinary[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    summonFrom {
      case b: BinaryInstanceBuilder[F] =>
        UnionDeriverImpl.deriveBinaryWithBuilder[F, T](using m, b)
      case _ =>
        compiletime.error("Binary derivation requires an explicit BinaryInstanceBuilder")
    }

  inline def derive[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    summonFrom {
      case b: BinaryInstanceBuilder[F] =>
        UnionDeriverImpl.deriveBinaryWithBuilder[F, T](using m, b)
      case b: ContravariantInstanceBuilder[F] =>
        UnionDeriverImpl.deriveContravariantWithBuilder[F, T](using m, b)
      case b: CovariantInstanceBuilder[F] =>
        UnionDeriverImpl.deriveCovariantWithBuilder[F, T](using m, b)
      case _ =>
        UnionDeriverImpl.deriveAutoSAM[F, T](using m)
    }
