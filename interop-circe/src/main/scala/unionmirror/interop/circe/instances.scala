package unionmirror.interop.circe

import scala.deriving.Mirror

import io.circe.{ Decoder, Encoder }

import unionmirror.UnionDeriver

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.AsInstanceOf"))
object instances:
  given UnionDeriver.ContravariantInstanceBuilder[Encoder] =
    new UnionDeriver.ContravariantInstanceBuilder[Encoder]:
      def build[T](dispatch: T => Encoder[Any]): Encoder[T] =
        Encoder.instance { (t: T) =>
          val enc = dispatch(t)
          enc.apply(t)
        }

  inline given [T] => Mirror.SumOf[T] => Encoder[T] =
    UnionDeriver.deriveContravariant[Encoder, T]

  given UnionDeriver.CovariantInstanceBuilder[Decoder] =
    new UnionDeriver.CovariantInstanceBuilder[Decoder]:
      def build[T](elems: IndexedSeq[Decoder[Any]]): Decoder[T] =
        Decoder.instance { (c: io.circe.HCursor) =>
          val failures = scala.collection.mutable.ListBuffer.empty[io.circe.DecodingFailure]
          val n = elems.length

          @scala.annotation.tailrec
          def loop(idx: Int): Decoder.Result[T] =
            if idx >= n then
              val msg =
                if failures.isEmpty then "No decoders configured"
                else
                  failures
                    .iterator
                    .map(_.message)
                    .mkString("No decoder succeeded; tried: [", " | ", "]")
              Left(io.circe.DecodingFailure(msg, c.history))
            else
              elems(idx).tryDecode(c) match
                case Right(value) => Right(value.asInstanceOf[T])
                case Left(err) =>
                  failures += err
                  loop(idx + 1)

          loop(0)
        }

  inline given [T] => Mirror.SumOf[T] => Decoder[T] =
    UnionDeriver.deriveCovariant[Decoder, T]
