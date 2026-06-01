package unionmirror.interop.circe

import io.circe.syntax.*
import io.circe.{ Decoder, Encoder, Json }
import munit.FunSuite
import unionmirror.UnionDeriver
import unionmirror.UnionMirror
import unionmirror.auto.given
import unionmirror.interop.circe.instances.given

class CirceInteropTests extends FunSuite:

  test("circe interop: Encoder[Cat] + Encoder[Dog] => Encoder[Cat | Dog]"):
    given Encoder[unionmirror.Cat] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Cat")))
    given Encoder[unionmirror.Dog] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Dog")))

    val enc = summon[Encoder[unionmirror.Cat | unionmirror.Dog]]
    assertEquals(obtained = unionmirror.Cat("m").asJson(using enc).noSpaces, expected = "{\"type\":\"Cat\"}")
    assertEquals(obtained = unionmirror.Dog("b").asJson(using enc).noSpaces, expected = "{\"type\":\"Dog\"}")

  test("circe interop: Decoder via summon"):
    case class TestCat(name: String)
    case class TestDog(age: Int)
    type TestUnion = TestCat | TestDog

    given Decoder[TestCat] =
      Decoder.instance(c => c.downField("name").as[String].map(TestCat(_)))
    given Decoder[TestDog] =
      Decoder.instance(c => c.downField("age").as[Int].map(TestDog(_)))

    given m: scala.deriving.Mirror.SumOf[TestUnion] = UnionMirror.synth[TestUnion]

    val decoder = summon[Decoder[TestUnion]]
    val res1 = decoder.decodeJson(Json.obj("name" -> "Whiskers".asJson))
    assertEquals(res1, Right(TestCat("Whiskers")).map(identity[TestUnion]))

    val res2 = decoder.decodeJson(Json.obj("age" -> 5.asJson))
    assertEquals(res2, Right(TestDog(5)).map(identity[TestUnion]))

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
