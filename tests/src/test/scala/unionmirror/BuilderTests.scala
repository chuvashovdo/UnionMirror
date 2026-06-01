package unionmirror

final class BuilderTests extends munit.FunSuite:

  test("custom ContravariantInstanceBuilder: Logger[Int | String | Boolean]"):
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

    given m: scala.deriving.Mirror.SumOf[Int | String | Boolean] =
      UnionMirror.synth[Int | String | Boolean]
    val logger = UnionDeriver.deriveContravariant[Logger, Int | String | Boolean]

    assertEquals(logger.log(42), "[CUSTOM]LOG-INT:42")
    assertEquals(logger.log("hello"), "[CUSTOM]LOG-STR:hello")
    assertEquals(logger.log(true), "[CUSTOM]LOG-BOOL:true")

  test("custom CovariantInstanceBuilder: Factory[Int | String | Boolean]"):
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

    given m: scala.deriving.Mirror.SumOf[Int | String | Boolean] =
      UnionMirror.synth[Int | String | Boolean]
    val factory = UnionDeriver.deriveCovariant[Factory, Int | String | Boolean]

    assertEquals(factory.create("42"), 42)
    assertEquals(factory.create("hello"), "hello")
    assertEquals(factory.create("true"), true)
    assertEquals(factory.create("123"), 123)
