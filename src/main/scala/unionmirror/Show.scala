package unionmirror

import scala.compiletime.{ erasedValue, summonInline }
import scala.deriving.Mirror

trait Show[-A]:
  def show(a: A): String

object Show:
  def apply[A](
    using
    A: Show[A]
  ): Show[A] =
    A

  given Show[Int]:
    def show(a: Int): String =
      a.toString

  given Show[Long]:
    def show(a: Long): String =
      a.toString

  given Show[String]:
    def show(a: String): String =
      a

  inline given derived[T](
    using
    m: Mirror.Of[T]
  ): Show[T] =
    inline m match
      case s: Mirror.SumOf[T] =>
        derivedSum(using s)
      case _ =>
        compiletime.error("Show.derived currently supports only sum types")

  inline private def derivedSum[T](
    using
    m: Mirror.SumOf[T]
  ): Show[T] =
    val elems = summonAll[m.MirroredElemTypes]
    new Show[T]:
      def show(a: T): String =
        val ord = m.ordinal(a)
        elems(ord).show(a.asInstanceOf)

  inline private def summonAll[Ts <: Tuple]: Array[Show[Any]] =
    inline erasedValue[Ts] match
      case _: EmptyTuple =>
        Array.empty
      case _: (t *: ts) =>
        val head = summonInline[Show[t]].asInstanceOf[Show[Any]]
        head +: summonAll[ts]
