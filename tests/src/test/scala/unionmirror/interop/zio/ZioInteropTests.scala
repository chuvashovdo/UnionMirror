package unionmirror.interop.zio

import munit.FunSuite
import unionmirror.UnionDeriver
import unionmirror.auto.given
import zio.prelude.*

class ZioInteropTests extends FunSuite:
  test("derive Equal for Int | String"):
    import unionmirror.interop.zio.instances.given

    given Equal[Int] = Equal.make(_ == _)
    given Equal[String] = Equal.make(_ == _)

    val eq = UnionDeriver.derive[Equal, Int | String]
    assert(eq.equal(1, 1))
    assert(!eq.equal(1, 2))
    assert(!eq.equal(1, "1"))
    assert(eq.equal("a", "a"))

  test("derive Equal for Int | String | Boolean"):
    import unionmirror.interop.zio.instances.given

    given Equal[Int] = Equal.make(_ == _)
    given Equal[String] = Equal.make(_ == _)
    given Equal[Boolean] = Equal.make(_ == _)

    val eq = UnionDeriver.derive[Equal, Int | String | Boolean]
    assert(eq.equal(1, 1))
    assert(!eq.equal(1, "1"))
    assert(!eq.equal(1, true))
    assert(eq.equal(true, true))

  test("derive Hash for Int | String"):
    import unionmirror.interop.zio.instances.given

    @scala.annotation.unused given Equal[Int] = Equal.make(_ == _)
    given Hash[Int] = Hash.make(_.hashCode, _ == _)
    @scala.annotation.unused given Equal[String] = Equal.make(_ == _)
    given Hash[String] = Hash.make(_.hashCode, _ == _)

    val hash = UnionDeriver.derive[Hash, Int | String]
    val h1 = hash.hash(42)
    val h2 = hash.hash("42")
    assert(h1 != h2)
