package unionmirror

import cats.{ Eq, Order }
import io.circe.syntax.*
import io.circe.{ Decoder, Encoder, Json }

final class InteropTests extends munit.FunSuite:
  import unionmirror.auto.given
  import unionmirror.interop.cats.instances.given
  import unionmirror.interop.circe.instances.given

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
    import cats.syntax.all.*

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

  test("circe interop: Encoder[Cat] + Encoder[Dog] => Encoder[Cat | Dog]"):
    given Encoder[Cat] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Cat")))
    given Encoder[Dog] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Dog")))

    val enc = summon[Encoder[Cat | Dog]]
    assertEquals(obtained = Cat("m").asJson(using enc).noSpaces, expected = "{\"type\":\"Cat\"}")
    assertEquals(obtained = Dog("b").asJson(using enc).noSpaces, expected = "{\"type\":\"Dog\"}")

  test("circe deep interop: Decoder[LocalCat | LocalDog | Int]"):
    case class LocalCat(catName: String)
    case class LocalDog(dogName: String)
    type LocalUnion = LocalCat | LocalDog | Int

    given Decoder[LocalCat] =
      Decoder.instance { c =>
        c.downField("catName").as[String].map(LocalCat(_))
      }
    given Decoder[LocalDog] =
      Decoder.instance { c =>
        c.downField("dogName").as[String].map(LocalDog(_))
      }

    given m: scala.deriving.Mirror.SumOf[LocalUnion] = UnionMirror.synth[LocalUnion]
    val decoder = UnionDeriver.deriveCovariant[Decoder, LocalUnion]

    val res1 = decoder.decodeJson(Json.obj("catName" -> "Mishu".asJson))
    assertEquals(res1, Right(LocalCat("Mishu")).map(identity[LocalUnion]))

    val res2 = decoder.decodeJson(Json.obj("dogName" -> "Rex".asJson))
    assertEquals(res2, Right(LocalDog("Rex")).map(identity[LocalUnion]))

    val res3 = decoder.decodeJson(42.asJson)
    assertEquals(res3, Right(42).map(identity[LocalUnion]))

    assert(decoder.decodeJson(Json.obj("unknown" -> 1.asJson)).isLeft)

  test("circe encoder interop: Encoder[LocalCat | LocalDog | Int]"):
    case class LocalCat(catName: String)
    case class LocalDog(dogName: String)
    type LocalUnion = LocalCat | LocalDog | Int

    given Encoder[LocalCat] =
      Encoder.instance(c => Json.obj("type" -> "Cat".asJson, "catName" -> c.catName.asJson))
    given Encoder[LocalDog] =
      Encoder.instance(d => Json.obj("type" -> "Dog".asJson, "dogName" -> d.dogName.asJson))

    given m: scala.deriving.Mirror.SumOf[LocalUnion] = UnionMirror.synth[LocalUnion]
    val encoder = UnionDeriver.deriveContravariant[Encoder, LocalUnion]

    val json1 = (LocalCat("Mishu"): LocalUnion).asJson(using encoder)
    assertEquals(json1.noSpaces, """{"type":"Cat","catName":"Mishu"}""")

    val json2 = (LocalDog("Rex"): LocalUnion).asJson(using encoder)
    assertEquals(json2.noSpaces, """{"type":"Dog","dogName":"Rex"}""")

    val json3 = (42: LocalUnion).asJson(using encoder)
    assertEquals(json3.noSpaces, "42")
