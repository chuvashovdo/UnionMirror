package unionmirror

import scala.deriving.Mirror

final class ExampleSuite extends munit.DisciplineSuite:
  test("Show derived for union via UnionMirror"):
    import Show.given

    val u1: Int | String | Long = 1
    val u2: Int | String | Long = "ok"
    val u3: Int | String | Long = 2L

    val m = UnionMirror.synth[Int | String | Long]
    val s = Show.derived(using m)

    assertEquals(s.show(u1), "1")
    assertEquals(s.show(u2), "ok")
    assertEquals(s.show(u3), "2")
