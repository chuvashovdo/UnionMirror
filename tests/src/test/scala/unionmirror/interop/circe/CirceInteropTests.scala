package unionmirror.interop.circe

import scala.annotation.experimental

import io.circe.syntax.*
import io.circe.{ Decoder, Encoder, Json }
import munit.FunSuite
import unionmirror.UnionDeriver
import unionmirror.UnionMirror
import unionmirror.auto.given
import unionmirror.interop.circe.instances.given

case class TestCat(name: String)
case class TestDog(name: String)
case class DecoderCat(catName: String)
case class DecoderDog(dogName: String)
case class EncoderCat(catName: String)
case class EncoderDog(dogName: String)

@experimental class CirceInteropTests extends FunSuite:
  test("circe interop: Encoder[Cat] + Encoder[Dog] => Encoder[Cat | Dog]"):
    given Encoder[TestCat] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Cat")))
    given Encoder[TestDog] =
      Encoder.instance(_ => io.circe.Json.obj("type" -> io.circe.Json.fromString("Dog")))

    val enc = summon[Encoder[TestCat | TestDog]]
    assertEquals((TestCat("m"): TestCat | TestDog).asJson(using enc).noSpaces, "{\"type\":\"Cat\"}")
    assertEquals((TestDog("b"): TestCat | TestDog).asJson(using enc).noSpaces, "{\"type\":\"Dog\"}")

  test("circe interop: Decoder via summon"):
    case class JsonCat(name: String)
    case class JsonDog(age: Int)
    type TestUnion = JsonCat | JsonDog

    given Decoder[JsonCat] =
      Decoder.instance(c => c.downField("name").as[String].map(JsonCat(_)))
    given Decoder[JsonDog] =
      Decoder.instance(c => c.downField("age").as[Int].map(JsonDog(_)))

    given m: scala.deriving.Mirror.SumOf[TestUnion] = UnionMirror.synth[TestUnion]

    val decoder = summon[Decoder[TestUnion]]
    val res1 = decoder.decodeJson(Json.obj("name" -> "Whiskers".asJson))
    assertEquals(res1, Right(JsonCat("Whiskers")).map(identity[TestUnion]))

    val res2 = decoder.decodeJson(Json.obj("age" -> 5.asJson))
    assertEquals(res2, Right(JsonDog(5)).map(identity[TestUnion]))

  test("circe deep interop: Decoder[LocalCat | LocalDog | Int]"):
    type LocalUnion = DecoderCat | DecoderDog | Int

    given Decoder[DecoderCat] =
      Decoder.instance { c =>
        c.downField("catName").as[String].map(DecoderCat(_))
      }
    given Decoder[DecoderDog] =
      Decoder.instance { c =>
        c.downField("dogName").as[String].map(DecoderDog(_))
      }

    given m: scala.deriving.Mirror.SumOf[LocalUnion] = UnionMirror.synth[LocalUnion]
    val decoder = UnionDeriver.deriveCovariant[Decoder, LocalUnion]

    val res1 = decoder.decodeJson(Json.obj("catName" -> "Mishu".asJson))
    assertEquals(res1, Right(DecoderCat("Mishu")).map(identity[LocalUnion]))

    val res2 = decoder.decodeJson(Json.obj("dogName" -> "Rex".asJson))
    assertEquals(res2, Right(DecoderDog("Rex")).map(identity[LocalUnion]))

    val res3 = decoder.decodeJson(42.asJson)
    assertEquals(res3, Right(42).map(identity[LocalUnion]))

    assert(decoder.decodeJson(Json.obj("unknown" -> 1.asJson)).isLeft)

  test("circe encoder interop: Encoder[LocalCat | LocalDog | Int]"):
    type LocalUnion = EncoderCat | EncoderDog | Int

    given Encoder[EncoderCat] =
      Encoder.instance(c => Json.obj("type" -> "Cat".asJson, "catName" -> c.catName.asJson))
    given Encoder[EncoderDog] =
      Encoder.instance(d => Json.obj("type" -> "Dog".asJson, "dogName" -> d.dogName.asJson))

    given m: scala.deriving.Mirror.SumOf[LocalUnion] = UnionMirror.synth[LocalUnion]
    val encoder = UnionDeriver.deriveContravariant[Encoder, LocalUnion]

    val json1 = (EncoderCat("Mishu"): LocalUnion).asJson(using encoder)
    assertEquals(json1.noSpaces, """{"type":"Cat","catName":"Mishu"}""")

    val json2 = (EncoderDog("Rex"): LocalUnion).asJson(using encoder)
    assertEquals(json2.noSpaces, """{"type":"Dog","dogName":"Rex"}""")

    val json3 = (42: LocalUnion).asJson(using encoder)
    assertEquals(json3.noSpaces, "42")
