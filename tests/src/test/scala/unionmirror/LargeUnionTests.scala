package unionmirror

import scala.annotation.experimental

@SuppressWarnings(Array("unused"))
@experimental final class LargeUnionTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("large union: Show[Int | String | Boolean | ... (15 types)]"):
    trait Show[-T]:
      def show(t: T): String

    case class A(x: Int)
    case class B(x: Int)
    case class C(x: Int)
    case class D(x: Int)
    case class E(x: Int)
    case class F(x: Int)
    case class G(x: Int)
    case class H(x: Int)
    case class I(x: Int)
    case class J(x: Int)
    case class K(x: Int)
    case class L(x: Int)
    case class M(x: Int)
    case class N(x: Int)
    case class O(x: Int)

    given showA: Show[A] = (a: A) => s"A(${a.x})"
    given showB: Show[B] = (b: B) => s"B(${b.x})"
    given showC: Show[C] = (c: C) => s"C(${c.x})"
    given showD: Show[D] = (d: D) => s"D(${d.x})"
    given showE: Show[E] = (e: E) => s"E(${e.x})"
    given showF: Show[F] = (f: F) => s"F(${f.x})"
    given showG: Show[G] = (g: G) => s"G(${g.x})"
    given showH: Show[H] = (h: H) => s"H(${h.x})"
    given showI: Show[I] = (i: I) => s"I(${i.x})"
    given showJ: Show[J] = (j: J) => s"J(${j.x})"
    given showK: Show[K] = (k: K) => s"K(${k.x})"
    given showL: Show[L] = (l: L) => s"L(${l.x})"
    given showM: Show[M] = (m: M) => s"M(${m.x})"
    given showN: Show[N] = (n: N) => s"N(${n.x})"
    given showO: Show[O] = (o: O) => s"O(${o.x})"

    type LargeUnion = A | B | C | D | E | F | G | H | I | J | K | L | M | N | O
    val s = UnionDeriver.deriveContravariant[Show, LargeUnion]
    assertEquals(s.show(A(1)), "A(1)")
    assertEquals(s.show(O(15)), "O(15)")
    assertEquals(s.show(H(8)), "H(8)")

  case class T0(x: Int)
  case class T1(x: Int)
  case class T2(x: Int)
  case class T3(x: Int)
  case class T4(x: Int)

  test("fallback performance with multiple types"):
    trait Parser[+T]:
      def parse(s: String): T

    given Parser[T0] =
      (s: String) => if s == "T0" then T0(0) else throw new IllegalArgumentException("not T0")
    given Parser[T1] =
      (s: String) => if s == "T1" then T1(1) else throw new IllegalArgumentException("not T1")
    given Parser[T2] =
      (s: String) => if s == "T2" then T2(2) else throw new IllegalArgumentException("not T2")
    given Parser[T3] =
      (s: String) => if s == "T3" then T3(3) else throw new IllegalArgumentException("not T3")
    given Parser[T4] =
      (s: String) => if s == "T4" then T4(4) else throw new IllegalArgumentException("not T4")

    type TestUnion = T0 | T1 | T2 | T3 | T4
    given m: scala.deriving.Mirror.SumOf[TestUnion] = UnionMirror.synth[TestUnion]
    val p = UnionDeriver.deriveCovariant[Parser, TestUnion]

    assertEquals(p.parse("T0"), T0(0))
    assertEquals(p.parse("T4"), T4(4))
    intercept[RuntimeException](p.parse("T5"))
