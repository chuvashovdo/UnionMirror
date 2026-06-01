package unionmirror

import cats.Eq

final class BinaryInstanceTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("binary builder: Eq[Int | String]"):
    given UnionDeriver.BinaryInstanceBuilder[Eq] =
      new UnionDeriver.BinaryInstanceBuilder[Eq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
          Eq.instance { (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).eqv(x, y)
          }

    val eq = UnionDeriver.deriveBinary[Eq, Int | String]
    assert(eq.eqv(1, 1))
    assert(!eq.eqv(1, 2))
    assert(!eq.eqv(1, "1"))
    assert(eq.eqv("a", "a"))

  test("cats Hash interop: Hash[Int | String | Boolean]"):
    import cats.kernel.Hash

    given Hash[Int] = Hash.fromUniversalHashCode[Int]
    given Hash[String] = Hash.fromUniversalHashCode[String]
    given Hash[Boolean] = Hash.fromUniversalHashCode[Boolean]

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
              ox == oy && elems(ox).eqv(x, y)

    given m: scala.deriving.Mirror.SumOf[Int | String | Boolean] =
      UnionMirror.synth[Int | String | Boolean]
    val hash = UnionDeriver.deriveBinary[Hash, Int | String | Boolean]

    val h1 = hash.hash(42)
    val h2 = hash.hash(42)
    assertEquals(h1, h2)

    val h3 = hash.hash("hello")
    val h4 = hash.hash(true)
    assert(h1 != h3 || h1 != h4 || h3 != h4)

  test("adapter derivation: explicit Mirror usage"):
    trait MyEq[T]:
      def isSame(x: T, y: T): Boolean

    given MyEq[Int] = (x, y) => x == y
    given MyEq[String] = (x, y) => x == y

    inline def deriveEq[T](
      using
      m: scala.deriving.Mirror.SumOf[T],
      @scala.annotation.unused builder: UnionDeriver.BinaryInstanceBuilder[MyEq],
    ): MyEq[T] =
      UnionDeriver.deriveBinary[MyEq, T]

    given UnionDeriver.BinaryInstanceBuilder[MyEq] =
      new UnionDeriver.BinaryInstanceBuilder[MyEq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[MyEq[Any]]): MyEq[T] =
          new MyEq[T]:
            def isSame(x: T, y: T): Boolean =
              val ox = ordinal(x)
              val oy = ordinal(y)
              ox == oy && elems(ox).isSame(x, y)

    given m: scala.deriving.Mirror.SumOf[Int | String] = UnionMirror.synth[Int | String]
    val eq = deriveEq[Int | String]

    assertEquals(eq.isSame(1.asInstanceOf[Int | String], 1.asInstanceOf[Int | String]), true)
    assertEquals(eq.isSame(1.asInstanceOf[Int | String], 2.asInstanceOf[Int | String]), false)
    assertEquals(eq.isSame(1.asInstanceOf[Int | String], "1".asInstanceOf[Int | String]), false)
    assertEquals(eq.isSame("a".asInstanceOf[Int | String], "a".asInstanceOf[Int | String]), true)
