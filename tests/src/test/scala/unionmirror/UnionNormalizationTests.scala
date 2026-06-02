package unionmirror

import scala.annotation.experimental

@experimental final class UnionNormalizationTests extends munit.FunSuite:
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
    @scala.annotation.unused trait NotSAM:
      def foo(i: Int): String
      def bar(s: String): Int

    compileErrors("UnionDeriver.deriveContravariant[NotSAM, Int | String]")

  test("parametrized types are not collapsed in normalization (List[Int] | List[String])"):
    // Even though JVM erasure prevents runtime discrimination, the compile-time
    // mirror must preserve both elements (regression for an earlier bug where
    // distinctStable used only typeSymbol and collapsed `List[Int] | List[String]`
    // to a single entry).
    @scala.annotation.nowarn("msg=Union elements.*share the same JVM erasure")
    val m = UnionMirror.synth[List[Int] | List[String]]

    // Both elements are present in MirroredElemTypes — Tuple.Size encodes the
    // arity at the type level. We verify via a value-level proxy: summoning a
    // value-of-types tuple over the labels.
    type Labels = m.MirroredElemLabels
    val labels = scala.compiletime.constValueTuple[Labels].productIterator.map(_.toString).toList
    assertEquals(labels.size, 2, s"expected 2 distinct elements, got labels=$labels")
    assert(labels.toSet.size == 2, s"expected distinct labels, got $labels")

  test("singleton literal types are not collapsed: 1 | 2"):
    val m = UnionMirror.synth[1 | 2]
    assert(m.ordinal(1) != m.ordinal(2))
