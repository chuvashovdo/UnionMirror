package unionmirror.interop.cats

import scala.deriving.Mirror

import cats.Show

import unionmirror.UnionDeriver

object instances:
  inline given [T](
    using
    Mirror.SumOf[T]
  ): Show[T] =
    UnionDeriver.deriveContravariant[Show, T]
