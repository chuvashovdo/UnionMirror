package unionmirror.interop.circe

import scala.deriving.Mirror

import io.circe.Encoder

import unionmirror.UnionDeriver

object instances:
  given UnionDeriver.ContravariantInstanceBuilder[Encoder] =
    new UnionDeriver.ContravariantInstanceBuilder[Encoder]:
      def build[T](dispatch: T => Encoder[Any]): Encoder[T] =
        Encoder.instance { (t: T) =>
          val enc = dispatch(t)
          enc.apply(t)
        }

  inline given [T](
    using
    Mirror.SumOf[T]
  ): Encoder[T] =
    UnionDeriver.deriveContravariant[Encoder, T]
