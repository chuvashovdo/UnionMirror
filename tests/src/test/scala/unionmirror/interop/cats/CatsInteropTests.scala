package unionmirror.interop.cats

import scala.annotation.experimental

import cats.{ Eq, Order }
import munit.FunSuite
import unionmirror.UnionMirror
import unionmirror.auto.given

case class TestCat(name: String)
case class TestDog(name: String)

@experimental class CatsInteropTests extends FunSuite:
  test("cats interop: Show[Cat] + Show[Dog] => Show[Cat | Dog]"):
    import cats.Show as CatsShow
    import unionmirror.interop.cats.instances.showForUnion

    given CatsShow[TestCat] = CatsShow.show(c => s"cat:${c.name}")
    given CatsShow[TestDog] = CatsShow.show(d => s"dog:${d.name}")

    val s = summon[CatsShow[TestCat | TestDog]]
    assertEquals(s.show(TestCat("m")), "cat:m")
    assertEquals(s.show(TestDog("b")), "dog:b")

  test("cats interop: Eq[Cat] + Eq[Dog] => Eq[Cat | Dog]"):
    import unionmirror.interop.cats.instances.{ eqBuilder, eqForUnion }

    given Eq[TestCat] = Eq.by[TestCat, String](_.name)
    given Eq[TestDog] = Eq.by[TestDog, String](_.name)

    val eq = summon[Eq[TestCat | TestDog]]
    assert(eq.eqv(TestCat("m"), TestCat("m")), "")
    assert(!eq.eqv(TestCat("m"), TestCat("x")), "")
    assert(!eq.eqv(TestCat("m"), TestDog("m")), "")
    assert(eq.eqv(TestDog("b"), TestDog("b")), "")

  test("cats interop: Order[Cat] + Order[Dog] => Order[Cat | Dog]"):
    import unionmirror.interop.cats.instances.{ orderBuilder, orderForUnion }

    given Order[TestCat] = Order.by[TestCat, String](_.name)
    given Order[TestDog] = Order.by[TestDog, String](_.name)

    val ord = summon[Order[TestCat | TestDog]]
    assert(ord.compare(TestCat("a"), TestCat("b")) < 0, "")
    assert(ord.compare(TestDog("a"), TestDog("b")) < 0, "")
    assert(ord.compare(TestCat("z"), TestDog("a")) < 0, "")

  test("cats deep interop: Order[Int | String | Boolean]"):
    import unionmirror.interop.cats.instances.{ orderBuilder, orderForUnion }

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
