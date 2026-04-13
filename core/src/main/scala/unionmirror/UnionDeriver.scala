package unionmirror

import scala.compiletime
import scala.compiletime.summonFrom
import scala.deriving.Mirror

import unionmirror.internal.UnionDeriverImpl

object UnionDeriver:
  trait ContravariantInstanceBuilder[F[_]]:
    def build[T](dispatch: T => F[Any]): F[T]

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
    compiletime.error("UnionDeriver.deriveCovariant is not implemented yet")

  inline def deriveBinary[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    compiletime.error("UnionDeriver.deriveBinary is not implemented yet")
