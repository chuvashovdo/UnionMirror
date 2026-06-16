package unionmirror

import scala.annotation.experimental

@experimental final class AutoDeriveTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("derive contravariant SAM: Printer[Int | String]"):
    trait Printer[-T]:
      def print(value: T): String

    given Printer[Int] = (value: Int) => s"int:$value"
    given Printer[String] = (value: String) => s"str:$value"

    val p = UnionDeriver.derive[Printer, Int | String]
    assertEquals(p.print(42), "int:42")
    assertEquals(p.print("hello"), "str:hello")

  test("derive with contravariant builder: Logger[Int | String]"):
    trait Logger[-T]:
      def log(value: T): String

    given Logger[Int] = (value: Int) => s"int:$value"
    given Logger[String] = (value: String) => s"str:$value"

    given UnionDeriver.ContravariantInstanceBuilder[Logger] =
      new UnionDeriver.ContravariantInstanceBuilder[Logger]:
        def build[T](dispatch: T => Logger[Any]): Logger[T] =
          new Logger[T]:
            def log(value: T): String =
              dispatch(value).log(value)

    val logger = UnionDeriver.derive[Logger, Int | String]
    assertEquals(logger.log(42), "int:42")
    assertEquals(logger.log("hello"), "str:hello")

  test("derive with binary builder: Eq[Int | String]"):
    import cats.Eq

    given Eq[Int] = Eq.fromUniversalEquals[Int]
    given Eq[String] = Eq.fromUniversalEquals[String]

    given UnionDeriver.BinaryInstanceBuilder[Eq] =
      new UnionDeriver.BinaryInstanceBuilder[Eq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
          Eq.instance { (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).eqv(x, y)
          }

    val eq = UnionDeriver.derive[Eq, Int | String]
    assert(eq.eqv(1, 1))
    assert(!eq.eqv(1, 2))
    assert(!eq.eqv(1, "1"))

  test("invariant SAM: Processor[Int | String] - may fail"):
    trait Processor[T]:
      def process(value: T): String

    given Processor[Int] = (value: Int) => s"int:$value"
    given Processor[String] = (value: String) => s"str:$value"

    val p = UnionDeriver.derive[Processor, Int | String]
    assertEquals(p.process(42), "int:42")
    assertEquals(p.process("hello"), "str:hello")

  test("derive vs deriveContravariant - equivalence"):
    trait Printer[-T]:
      def print(value: T): String

    given Printer[Int] = (value: Int) => s"int:$value"
    given Printer[String] = (value: String) => s"str:$value"

    val p1 = UnionDeriver.deriveContravariant[Printer, Int | String]
    val p2 = UnionDeriver.derive[Printer, Int | String]

    assertEquals(p1.print(42), p2.print(42))
    assertEquals(p1.print("hello"), p2.print("hello"))

  test("auto derive: covariant SAM Parser[Int | Boolean] from SamTests"):
    trait Parser[+T]:
      def parse(s: String): T

    given Parser[Int] = (s: String) => s.toInt
    given Parser[Boolean] = (s: String) => s.toBoolean

    val p = UnionDeriver.derive[Parser, Int | Boolean]
    assertEquals(p.parse("123"), 123)
    assertEquals(p.parse("true"), true)
    intercept[RuntimeException](p.parse("not-a-number"))

  test("auto derive: covariant SAM custom Parser[Cat | Dog] from SamTests"):
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

    val p = UnionDeriver.derive[Parser, Cat | Dog]
    assertEquals(p.parse("cat:m"), Cat("m"))
    assertEquals(p.parse("dog:b"), Dog("b"))
    intercept[RuntimeException](p.parse("bird:x"))

  test("auto derive: ContravariantInstanceBuilder from BuilderTests"):
    trait Logger[-T]:
      def log(value: T): String

    given Logger[Int] = (i: Int) => s"LOG-INT:$i"
    given Logger[String] = (s: String) => s"LOG-STR:$s"
    given Logger[Boolean] = (b: Boolean) => s"LOG-BOOL:$b"

    given UnionDeriver.ContravariantInstanceBuilder[Logger] =
      new UnionDeriver.ContravariantInstanceBuilder[Logger]:
        def build[T](dispatch: T => Logger[Any]): Logger[T] =
          new Logger[T]:
            def log(value: T): String =
              s"[CUSTOM]${dispatch(value).log(value)}"

    val logger = UnionDeriver.derive[Logger, Int | String | Boolean]
    assertEquals(logger.log(42), "[CUSTOM]LOG-INT:42")
    assertEquals(logger.log("hello"), "[CUSTOM]LOG-STR:hello")
    assertEquals(logger.log(true), "[CUSTOM]LOG-BOOL:true")

  test("auto derive: CovariantInstanceBuilder from BuilderTests"):
    trait Factory[+T]:
      def create(s: String): T

    given Factory[Int] = (s: String) => s.toInt
    given Factory[String] = (s: String) => s
    given Factory[Boolean] = (s: String) => s.toBoolean

    given UnionDeriver.CovariantInstanceBuilder[Factory] =
      new UnionDeriver.CovariantInstanceBuilder[Factory]:
        def build[T](elems: IndexedSeq[Factory[Any]]): Factory[T] =
          new Factory[T]:
            def create(s: String): T =
              import scala.util.control.Breaks.*
              var result: Option[T] = None
              var lastError: Option[Throwable] = None
              breakable:
                for elem <- elems do
                  try
                    val r = elem.create(s).asInstanceOf[T]
                    result = Some(r)
                    break()
                  catch case e => lastError = Some(e)
              result match
                case Some(r) => r
                case None =>
                  throw new RuntimeException(
                    s"All factories failed: ${lastError.map(_.toString).getOrElse("unknown")}"
                  )

    val factory = UnionDeriver.derive[Factory, Int | String | Boolean]
    assertEquals(factory.create("42"), 42)
    assertEquals(factory.create("hello"), "hello")
    assertEquals(factory.create("true"), true)
    assertEquals(factory.create("123"), 123)

  test("auto derive: BinaryInstanceBuilder Eq from BinaryInstanceTests"):
    import cats.Eq

    given Eq[Int] = Eq.fromUniversalEquals[Int]
    given Eq[String] = Eq.fromUniversalEquals[String]

    given UnionDeriver.BinaryInstanceBuilder[Eq] =
      new UnionDeriver.BinaryInstanceBuilder[Eq]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
          Eq.instance { (x, y) =>
            val ox = ordinal(x)
            val oy = ordinal(y)
            ox == oy && elems(ox).eqv(x, y)
          }

    val eq = UnionDeriver.derive[Eq, Int | String]
    assert(eq.eqv(1, 1))
    assert(!eq.eqv(1, 2))
    assert(!eq.eqv(1, "1"))
    assert(eq.eqv("a", "a"))

  test("API example: Loggable[Int | String | Boolean]"):
    trait Loggable[-T]:
      def log(value: T): String

    given Loggable[Int] = (i: Int) => s"INT:$i"
    given Loggable[String] = (s: String) => s"STR:$s"
    given Loggable[Boolean] = (b: Boolean) => s"BOOL:$b"

    val loggable = UnionDeriver.derive[Loggable, Int | String | Boolean]
    assertEquals(loggable.log(42), "INT:42")
    assertEquals(loggable.log("hello"), "STR:hello")
    assertEquals(loggable.log(true), "BOOL:true")

  test("typeclass-local given works for multiple unions"):
    trait Loggable[-T]:
      def log(value: T): String

    given Loggable[Int] = (i: Int) => s"INT:$i"
    given Loggable[String] = (s: String) => s"STR:$s"
    given Loggable[Boolean] = (b: Boolean) => s"BOOL:$b"

    inline given [T](using scala.deriving.Mirror.SumOf[T]): Loggable[T] =
      UnionDeriver.derive[Loggable, T]

    val loggable2 = summon[Loggable[Int | String]]
    val loggable3 = summon[Loggable[Int | String | Boolean]]

    assertEquals(loggable2.log(42), "INT:42")
    assertEquals(loggable2.log("hello"), "STR:hello")
    assertEquals(loggable3.log(42), "INT:42")
    assertEquals(loggable3.log("hello"), "STR:hello")
    assertEquals(loggable3.log(true), "BOOL:true")

  test("API example: Show[Int | String]"):
    trait Show[-T]:
      def show(value: T): String

    given Show[Int] = (i: Int) => s"int:$i"
    given Show[String] = (s: String) => s"str:$s"

    val show = UnionDeriver.derive[Show, Int | String]
    assertEquals(show.show(42), "int:42")
    assertEquals(show.show("hello"), "str:hello")

  test("automatic Mirror.SumOf via summon"):
    type TestUnion = Int | String | Boolean

    val mirror = summon[scala.deriving.Mirror.SumOf[TestUnion]]
    val ordinals = List(mirror.ordinal(42), mirror.ordinal("hello"), mirror.ordinal(true))
    assertEquals(ordinals.distinct.size, 3)
