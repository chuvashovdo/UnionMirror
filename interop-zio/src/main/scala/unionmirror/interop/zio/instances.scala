package unionmirror.interop.zio

import unionmirror.UnionDeriver
import zio.prelude.*

/** Instances for ZIO Prelude typeclasses */
object instances:
  given UnionDeriver.BinaryInstanceBuilder[Equal] =
    new UnionDeriver.BinaryInstanceBuilder[Equal]:
      def build[T](ordinal: T => Int, elems: List[Equal[Any]]): Equal[T] =
        Equal.make { (x, y) =>
          val ox = ordinal(x)
          val oy = ordinal(y)
          ox == oy && elems(ox).equal(x, y)
        }

  given UnionDeriver.BinaryInstanceBuilder[Hash] =
    new UnionDeriver.BinaryInstanceBuilder[Hash]:
      def build[T](ordinal: T => Int, elems: List[Hash[Any]]): Hash[T] =
        Hash.make(
          x =>
            val ox = ordinal(x)
            elems(ox).hash(x)
          ,
          (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).equal(x, y),
        )
