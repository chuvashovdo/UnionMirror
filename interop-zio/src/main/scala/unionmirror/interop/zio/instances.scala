package unionmirror.interop.zio

import scala.deriving.Mirror

import unionmirror.UnionDeriver
import zio.prelude.*

/** Instances for ZIO Prelude typeclasses */
object instances:
  given UnionDeriver.BinaryInstanceBuilder[Equal] =
    new UnionDeriver.BinaryInstanceBuilder[Equal]:
      def build[T](ordinal: T => Int, elems: IndexedSeq[Equal[Any]]): Equal[T] =
        Equal.make { (x, y) =>
          val ox = ordinal(x)
          val oy = ordinal(y)
          ox == oy && elems(ox).equal(x, y)
        }

  inline given [T] => Mirror.SumOf[T] => Equal[T] =
    UnionDeriver.deriveBinary[Equal, T]

  given UnionDeriver.BinaryInstanceBuilder[Hash] =
    new UnionDeriver.BinaryInstanceBuilder[Hash]:
      def build[T](ordinal: T => Int, elems: IndexedSeq[Hash[Any]]): Hash[T] =
        Hash.make(
          x =>
            val ox = ordinal(x)
            31 * ox + elems(ox).hash(x)
          ,
          (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).equal(x, y),
        )

  inline given [T] => Mirror.SumOf[T] => Hash[T] =
    UnionDeriver.deriveBinary[Hash, T]
