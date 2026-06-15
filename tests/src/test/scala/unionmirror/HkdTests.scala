package unionmirror

import scala.annotation.experimental

@experimental final class HkdTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("type lambda union: Mirror synthesis for [A] =>> List[A] | Int"):
    type Hkd = [A] =>> List[A] | Int

    given m: scala.deriving.Mirror.SumOf[Hkd[String]] =
      UnionMirror.synth[Hkd[String]]

    assert(m.ordinal(42) != m.ordinal(List("hello")))
    assert(m.ordinal(42) != m.ordinal(List("world")))

  test("type lambda union: Mirror synthesis stays stable across applications"):
    type Hkd = [A] =>> List[A] | Int

    val mString = UnionMirror.synth[Hkd[String]]
    val mInt = UnionMirror.synth[Hkd[Int]]

    assert(mString.ordinal(42) != mString.ordinal(List("hello")))
    assert(mInt.ordinal(42) != mInt.ordinal(List(1, 2, 3)))

  test("higher-kinded union alias: Mirror synthesis for Hkd[String]"):
    type Hkd[A] = List[A] | Int

    given m: scala.deriving.Mirror.SumOf[Hkd[String]] =
      UnionMirror.synth[Hkd[String]]

    assert(m.ordinal(42) != m.ordinal(List("hello")))

  test("higher-kinded union alias: Show[Hkd[String]]"):
    type Hkd[A] = List[A] | Int

    trait Show[-T]:
      def show(value: T): String

    given Show[Int] = (i: Int) => s"int:$i"
    given Show[List[String]] = (xs: List[String]) => s"list:${xs.mkString(",")}"

    val show = UnionDeriver.deriveContravariant[Show, Hkd[String]]

    assertEquals(show.show(42), "int:42")
    assertEquals(show.show(List("a", "b")), "list:a,b")

  test("type lambda union: Show[([A] =>> List[A] | Int)[String]]"):
    type Hkd = [A] =>> List[A] | Int

    trait Show[-T]:
      def show(value: T): String

    given Show[Int] = (i: Int) => s"int:$i"
    given Show[List[String]] = (xs: List[String]) => s"list:${xs.mkString(",")}"

    val show = UnionDeriver.deriveContravariant[Show, Hkd[String]]

    assertEquals(show.show(7), "int:7")
    assertEquals(show.show(List("x", "y")), "list:x,y")
    assertEquals(show.show(List("solo")), "list:solo")

  test("type lambda union: simple derive for Show[([A] =>> List[A] | Int)[String]]"):
    type Hkd = [A] =>> List[A] | Int

    trait Show[-T]:
      def show(value: T): String

    given Show[Int] = (i: Int) => s"int:$i"
    given Show[List[String]] = (xs: List[String]) => s"list:${xs.mkString(",")}"

    val show = UnionDeriver.derive[Show, Hkd[String]]

    assertEquals(show.show(5), "int:5")
    assertEquals(show.show(List("a", "b")), "list:a,b")

  test("type lambda union: Eq[([A] =>> List[A] | Int)[String]]"):
    import cats.Eq

    type Hkd = [A] =>> List[A] | Int

    given Eq[Int] = Eq.fromUniversalEquals[Int]
    given Eq[List[String]] = Eq.fromUniversalEquals[List[String]]

    given UnionDeriver.BinaryInstanceBuilder[Eq] =
      new UnionDeriver.BinaryInstanceBuilder[Eq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
          Eq.instance { (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).eqv(x, y)
          }

    val eq = UnionDeriver.deriveBinary[Eq, Hkd[String]]

    assert(eq.eqv(42, 42))
    assert(!eq.eqv(42, 43))
    assert(eq.eqv(List("a", "b"), List("a", "b")))
    assert(!eq.eqv(List("a"), List("b")))
    assert(!eq.eqv(42, List("42")))

  test("type lambda union: simple derive for Eq[([A] =>> List[A] | Int)[String]]"):
    import cats.Eq

    type Hkd = [A] =>> List[A] | Int

    given Eq[Int] = Eq.fromUniversalEquals[Int]
    given Eq[List[String]] = Eq.fromUniversalEquals[List[String]]

    given UnionDeriver.BinaryInstanceBuilder[Eq] =
      new UnionDeriver.BinaryInstanceBuilder[Eq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
          Eq.instance { (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).eqv(x, y)
          }

    val eq = UnionDeriver.derive[Eq, Hkd[String]]

    assert(eq.eqv(7, 7))
    assert(!eq.eqv(7, 8))
    assert(eq.eqv(List("x"), List("x")))
    assert(!eq.eqv(List("x"), List("y")))
