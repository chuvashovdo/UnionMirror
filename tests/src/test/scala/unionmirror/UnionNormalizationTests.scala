package unionmirror

final class UnionNormalizationTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("nested union and normalization: Show[Int | (String | Boolean)]"):
    trait Show[-T]:
      def show(t: T): String

    given Show[Int] = (i: Int) => s"i:$i"
    given Show[String] = (s: String) => s"s:$s"
    given Show[Boolean] = (b: Boolean) => s"b:$b"

    val s = UnionDeriver.deriveContravariant[Show, Int | (String | Boolean)]
    assertEquals(s.show(1), "i:1")
    assertEquals(s.show("a"), "s:a")
    assertEquals(s.show(true), "b:true")

  test("union normalization and ordering"):
    val m1 = UnionMirror.synth[String | Int]
    val m2 = UnionMirror.synth[Int | String]

    assertEquals(m1.ordinal(1), m2.ordinal(1))
    assertEquals(m1.ordinal("a"), m2.ordinal("a"))

    val m3 = UnionMirror.synth[Int | Int | String]
    assertEquals(m3.ordinal(1), m1.ordinal(1))
    assertEquals(m3.ordinal("a"), m1.ordinal("a"))

  test("ordinal stability across multiple calls"):
    val m1 = UnionMirror.synth[String | Int | Boolean]
    val m2 = UnionMirror.synth[String | Int | Boolean]

    assertEquals(m1.ordinal(1), m2.ordinal(1))
    assertEquals(m1.ordinal("a"), m2.ordinal("a"))
    assertEquals(m1.ordinal(true), m2.ordinal(true))

    assert(m1.ordinal(1) != m1.ordinal("a"))
    assert(m1.ordinal(1) != m1.ordinal(true))
    assert(m1.ordinal("a") != m1.ordinal(true))

  test("UnionMirror should fail for non-union types"):
    compileErrors("UnionMirror.synth[Int]")

  test("UnionDeriver should fail for non-SAM types without builder"):
    trait NotSAM:
      def foo(i: Int): String
      def bar(s: String): Int

    compileErrors("UnionDeriver.deriveContravariant[NotSAM, Int | String]")
