package unionmirror.interop.cats

import scala.deriving.Mirror

import cats.{ Eq, Hash, Order, Show }

import unionmirror.UnionDeriver

object instances:
  inline given [T] => Mirror.SumOf[T] => Show[T] =
    UnionDeriver.deriveContravariant[Show, T]

  given UnionDeriver.BinaryInstanceBuilder[Eq] =
    new UnionDeriver.BinaryInstanceBuilder[Eq]:
      def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
        new Eq[T]:
          def eqv(x: T, y: T): Boolean =
            val ox = ordinal(x)
            val oy = ordinal(y)
            if ox != oy then false
            else elems(ox).eqv(x, y)

  inline given [T] => Mirror.SumOf[T] => Eq[T] =
    UnionDeriver.deriveBinary[Eq, T]

  given UnionDeriver.BinaryInstanceBuilder[Order] =
    new UnionDeriver.BinaryInstanceBuilder[Order]:
      def build[T](ordinal: T => Int, elems: IndexedSeq[Order[Any]]): Order[T] =
        new Order[T]:
          def compare(x: T, y: T): Int =
            val ox = ordinal(x)
            val oy = ordinal(y)
            if ox != oy then Integer.compare(ox, oy)
            else elems(ox).compare(x, y)

  inline given [T] => Mirror.SumOf[T] => Order[T] =
    UnionDeriver.deriveBinary[Order, T]

  given UnionDeriver.BinaryInstanceBuilder[Hash] =
    new UnionDeriver.BinaryInstanceBuilder[Hash]:
      def build[T](ordinal: T => Int, elems: IndexedSeq[Hash[Any]]): Hash[T] =
        new Hash[T]:
          def hash(x: T): Int =
            val ox = ordinal(x)
            31 * ox + elems(ox).hash(x)
          def eqv(x: T, y: T): Boolean =
            val ox = ordinal(x)
            val oy = ordinal(y)
            if ox != oy then false
            else elems(ox).eqv(x, y)

  inline given [T] => Mirror.SumOf[T] => Hash[T] =
    UnionDeriver.deriveBinary[Hash, T]
