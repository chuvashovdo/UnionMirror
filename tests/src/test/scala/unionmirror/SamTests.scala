package unionmirror

final class SamTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("contravariant SAM: Printer[Int | String]"):
    trait Printer[-T]:
      def print(value: T): String

    given Printer[Int] = (value: Int) => s"int:$value"
    given Printer[String] = (value: String) => s"str:$value"

    val p = UnionDeriver.deriveContravariant[Printer, Int | String]
    assertEquals(p.print(42), "int:42")
    assertEquals(p.print("hello"), "str:hello")

  test("covariant SAM: Parser[Int | Boolean] fallback"):
    trait Parser[+T]:
      def parse(s: String): T

    given Parser[Int] = (s: String) => s.toInt
    given Parser[Boolean] = (s: String) => s.toBoolean

    val p = UnionDeriver.deriveCovariant[Parser, Int | Boolean]
    assertEquals(p.parse("123"), 123)
    assertEquals(p.parse("true"), true)
    intercept[RuntimeException](p.parse("not-a-number"))

  test("covariant SAM: custom Parser[Cat | Dog] fallback"):
    trait Parser[+T]:
      def parse(s: String): T

    given Parser[Cat] =
      new Parser[Cat]:
        def parse(s: String): Cat =
          if s.startsWith("cat:") then Cat(s.stripPrefix("cat:"))
          else throw new IllegalArgumentException("Not a cat")

    given Parser[Dog] =
      new Parser[Dog]:
        def parse(s: String): Dog =
          if s.startsWith("dog:") then Dog(s.stripPrefix("dog:"))
          else throw new IllegalArgumentException("Not a dog")

    val p = UnionDeriver.deriveCovariant[Parser, Cat | Dog]
    assertEquals(p.parse("cat:m"), Cat("m"))
    assertEquals(p.parse("dog:b"), Dog("b"))
    intercept[RuntimeException](p.parse("bird:x"))
