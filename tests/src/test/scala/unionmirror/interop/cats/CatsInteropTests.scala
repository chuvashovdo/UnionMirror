package unionmirror.interop.cats

import cats.{ Eq, Order }
import munit.FunSuite
import unionmirror.{ Cat, Dog }
import unionmirror.UnionMirror
import unionmirror.auto.given
import unionmirror.interop.cats.instances.given

class CatsInteropTests extends FunSuite:
  test("cats interop: Show[Cat] + Show[Dog] => Show[Cat | Dog]"):
    import cats.Show as CatsShow
    given CatsShow[Cat] = CatsShow.show(c => s"cat:${c.name}")
    given CatsShow[Dog] = CatsShow.show(d => s"dog:${d.name}")

    val s = summon[CatsShow[Cat | Dog]]
    assertEquals(obtained = s.show(Cat("m")), expected = "cat:m")
    assertEquals(obtained = s.show(Dog("b")), expected = "dog:b")

  test("cats interop: Eq[Cat] + Eq[Dog] => Eq[Cat | Dog]"):
    given Eq[Cat] = Eq.by[Cat, String](_.name)
    given Eq[Dog] = Eq.by[Dog, String](_.name)

    val eq = summon[Eq[Cat | Dog]]
    assert(eq.eqv(Cat("m"), Cat("m")))
    assert(!eq.eqv(Cat("m"), Cat("x")))
    assert(!eq.eqv(Cat("m"), Dog("m")))
    assert(eq.eqv(Dog("b"), Dog("b")))

  test("cats interop: Order[Cat] + Order[Dog] => Order[Cat | Dog]"):
    given Order[Cat] = Order.by[Cat, String](_.name)
    given Order[Dog] = Order.by[Dog, String](_.name)

    val ord = summon[Order[Cat | Dog]]
    assert(ord.compare(Cat("a"), Cat("b")) < 0)
    assert(ord.compare(Dog("a"), Dog("b")) < 0)
    assert(ord.compare(Cat("z"), Dog("a")) < 0)

  test("cats deep interop: Order[Int | String | Boolean]"):
    val m = UnionMirror.synth[Int | String | Boolean]
    val ord = summon[cats.kernel.Order[Int | String | Boolean]]

    val oInt = m.ordinal(1)
    val oStr = m.ordinal("a")
    val oBool = m.ordinal(true)

    assertEquals(ord.compare(1, "a"), Integer.compare(oInt, oStr))
    assertEquals(ord.compare("a", true), Integer.compare(oStr, oBool))

    assertEquals(ord.compare(1, 2), -1)
    assertEquals(ord.compare("b", "a"), 1)
    assertEquals(ord.compare(true, false), 1)
