package unionmirror.interop.circe

import scala.deriving.Mirror

import io.circe.{ Decoder, Encoder, Json }

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

  given UnionDeriver.CovariantInstanceBuilder[Decoder] =
    new UnionDeriver.CovariantInstanceBuilder[Decoder]:
      def build[T](elems: List[Decoder[Any]]): Decoder[T] =
        Decoder.instance { (c: io.circe.HCursor) =>
          elems.foldLeft[Decoder.Result[T]](
            Left(io.circe.DecodingFailure("No decoder succeeded", c.history))
          ) { (acc, decoder) =>
            acc match
              case Right(value) => Right(value)
              case Left(_) =>
                decoder.tryDecode(c) match
                  case Right(value) => Right(value.asInstanceOf[T])
                  case Left(_) => acc
          }
        }

  inline given [T](
    using
    Mirror.SumOf[T]
  ): Decoder[T] =
    UnionDeriver.deriveCovariant[Decoder, T]
