package unionmirror

import cats.Show as CatsShow
import io.circe.syntax.*
import io.circe.Encoder

final class ExampleSuite extends munit.FunSuite:
  import unionmirror.auto.given
  import unionmirror.interop.catsInterop.given
  import unionmirror.interop.circeInterop.given

  test("UnionMirror builds Mirror.SumOf for unions"):
    val m = UnionMirror.synth[Cat | Dog]
    assertEquals(m.ordinal(Cat("m")), 0)
    assertEquals(m.ordinal(Dog("b")), 1)

  test("cats interop: Show[Cat] + Show[Dog] => Show[Cat | Dog]"):
    given CatsShow[Cat] = CatsShow.show(c => s"cat:${c.name}")
    given CatsShow[Dog] = CatsShow.show(d => s"dog:${d.name}")

    val s = summon[CatsShow[Cat | Dog]]
    assertEquals(obtained = s.show(Cat("m")), expected = "cat:m")
    assertEquals(obtained = s.show(Dog("b")), expected = "dog:b")

  test("circe interop: Encoder[Cat] + Encoder[Dog] => Encoder[Cat | Dog]"):
    given Encoder[Cat] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Cat")))
    given Encoder[Dog] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Dog")))

    val enc = summon[Encoder[Cat | Dog]]
    assertEquals(obtained = Cat("m").asJson(using enc).noSpaces, expected = "{\"type\":\"Cat\"}")
    assertEquals(obtained = Dog("b").asJson(using enc).noSpaces, expected = "{\"type\":\"Dog\"}")
