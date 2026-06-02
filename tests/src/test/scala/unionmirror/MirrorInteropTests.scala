package unionmirror

import scala.annotation.experimental

@experimental final class MirrorInteropTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("system mirror interop: Show[MyEnum | (Cat | Dog)]"):
    trait Show[-T]:
      def show(t: T): String

    enum MyEnum:
      case A, B

    given showEnum: Show[MyEnum] = (e: MyEnum) => s"Enum(${e.toString})"
    given showCat: Show[Cat] = (c: Cat) => s"Cat(${c.name})"
    given showDog: Show[Dog] = (d: Dog) => s"Dog(${d.name})"

    type MixedUnion = MyEnum | Cat | Dog
    given m: scala.deriving.Mirror.SumOf[MixedUnion] = UnionMirror.synth[MixedUnion]
    val s = UnionDeriver.deriveContravariant[Show, MixedUnion]

    assertEquals(s.show(MyEnum.A), "Enum(A)")
    assertEquals(s.show(Cat("Mishu")), "Cat(Mishu)")
    assertEquals(s.show(Dog("Rex")), "Dog(Rex)")

  test("standard scala 3 enum mirror interop: Show[StandardEnum | Int]"):
    trait Show[-T]:
      def show(t: T): String

    enum StandardEnum:
      case ValueA, ValueB, ValueC

    given showEnum: Show[StandardEnum] = (e: StandardEnum) => s"Enum:${e.toString}"
    given showInt: Show[Int] = (i: Int) => s"Int:$i"

    type MixedUnion = StandardEnum | Int
    val s = UnionDeriver.deriveContravariant[Show, MixedUnion]

    assertEquals(s.show(StandardEnum.ValueA), "Enum:ValueA")
    assertEquals(s.show(StandardEnum.ValueB), "Enum:ValueB")
    assertEquals(s.show(42), "Int:42")

  test("standard scala 3 sealed trait mirror interop: Show[StandardSealed | String]"):
    trait Show[-T]:
      def show(t: T): String

    sealed trait StandardSealed:
      def value: String
    case class CaseA(value: String) extends StandardSealed
    case class CaseB(value: String) extends StandardSealed

    given showSealed: Show[StandardSealed] = (s: StandardSealed) => s"Sealed:${s.value}"
    given showString: Show[String] = (s: String) => s"Str:$s"

    type MixedUnion = StandardSealed | String
    val s = UnionDeriver.deriveContravariant[Show, MixedUnion]

    assertEquals(s.show(CaseA("test")), "Sealed:test")
    assertEquals(s.show(CaseB("hello")), "Sealed:hello")
    assertEquals(s.show("world"), "Str:world")
