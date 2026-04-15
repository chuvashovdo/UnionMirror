package unionmirror

final class AdvancedTypeTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("singleton types union: Show[1 | 'a' | true]"):
    trait Show[-T]:
      def show(t: T): String

    given showOne: Show[1] = (_: 1) => "one"
    given showA: Show["a"] = (_: "a") => "char-a"
    given showTrue: Show[true] = (_: true) => "boolean-true"

    val s = UnionDeriver.deriveContravariant[Show, 1 | "a" | true]
    assertEquals(s.show(1), "one")
    assertEquals(s.show("a"), "char-a")
    assertEquals(s.show(true), "boolean-true")

  test("union with objects: Show[MyObject1.type | MyObject2.type]"):
    trait Show[-T]:
      def show(t: T): String

    object MyObject1:
      val value =
        "obj1"
    object MyObject2:
      val value =
        "obj2"

    given showObj1: Show[MyObject1.type] = (_: MyObject1.type) => "Object1"
    given showObj2: Show[MyObject2.type] = (_: MyObject2.type) => "Object2"

    given m: scala.deriving.Mirror.SumOf[MyObject1.type | MyObject2.type] =
      UnionMirror.synth[MyObject1.type | MyObject2.type]
    val s = UnionDeriver.deriveContravariant[Show, MyObject1.type | MyObject2.type]
    assertEquals(s.show(MyObject1), "Object1")
    assertEquals(s.show(MyObject2), "Object2")

  test("parametrized traits union: Show[Container[Int] | Container[String] | Container[Boolean]]"):
    trait Show[-T]:
      def show(t: T): String

    trait Container[+A]:
      def value: A

    case class IntContainer(value: Int) extends Container[Int]
    case class StringContainer(value: String) extends Container[String]
    case class BooleanContainer(value: Boolean) extends Container[Boolean]

    given showIntCont: Show[IntContainer] = (c: IntContainer) => s"Int(${c.value})"
    given showStrCont: Show[StringContainer] = (c: StringContainer) => s"Str(${c.value})"
    given showBoolCont: Show[BooleanContainer] = (c: BooleanContainer) => s"Bool(${c.value})"

    type ContUnion = IntContainer | StringContainer | BooleanContainer
    val s = UnionDeriver.deriveContravariant[Show, ContUnion]
    assertEquals(s.show(IntContainer(42)), "Int(42)")
    assertEquals(s.show(StringContainer("hello")), "Str(hello)")
    assertEquals(s.show(BooleanContainer(true)), "Bool(true)")

  test("parametrized types union: Show[List[Int] | Option[String] | Vector[Int]]"):
    trait Show[-T]:
      def show(t: T): String

    given showListInt: Show[List[Int]] = (l: List[Int]) => s"List(${l.mkString(",")})"
    given showOptionString: Show[Option[String]] =
      (o: Option[String]) => s"Opt(${o.getOrElse("none")})"
    given showVectorInt: Show[Vector[Int]] = (v: Vector[Int]) => s"Vec(${v.mkString(",")})"

    type ParamUnion = List[Int] | Option[String] | Vector[Int]
    val s = UnionDeriver.deriveContravariant[Show, ParamUnion]
    assertEquals(s.show(List(1, 2, 3)), "List(1,2,3)")
    assertEquals(s.show(Some("hello")), "Opt(hello)")
    assertEquals(s.show(Vector(1, 2)), "Vec(1,2)")

  test("multi-param types union: Show[Either[String, Int] | (Int, String) | Map[String, Int]]"):
    trait Show[-T]:
      def show(t: T): String

    given Show[Either[String, Int]] = (e: Either[String, Int]) => e.fold(s => s"L:$s", i => s"R:$i")
    given Show[(Int, String)] = (t: (Int, String)) => s"(${t._1},${t._2})"
    given Show[Map[String, Int]] = (m: Map[String, Int]) => s"Map(${m.size})"

    type MultiParamUnion = Either[String, Int] | (Int, String) | Map[String, Int]
    val s = UnionDeriver.deriveContravariant[Show, MultiParamUnion]
    assertEquals(s.show(Left("error")), "L:error")
    assertEquals(s.show(Right(42)), "R:42")
    assertEquals(s.show((1, "a")), "(1,a)")
    assertEquals(s.show(Map("x" -> 1)), "Map(1)")

  test("union with Any: Show[Int | String | Any]"):
    trait Show[-T]:
      def show(t: T): String

    given showInt: Show[Int] = (i: Int) => s"int:$i"
    given showString: Show[String] = (s: String) => s"str:$s"
    given showAny: Show[Any] = (a: Any) => s"any:$a"

    given m: scala.deriving.Mirror.SumOf[Int | String | Any] = UnionMirror.synth[Int | String | Any]
    val s = UnionDeriver.deriveContravariant[Show, Int | String | Any]
    assertEquals(s.show(42), "int:42")
    assertEquals(s.show("hello"), "str:hello")
    assertEquals(s.show(true), "any:true")

  test("LSP with common methods: Show[Drawable | (Circle | Square))]"):
    trait Show[-T]:
      def show(t: T): String

    trait Drawable:
      def draw(): String
    trait Colorable:
      def color(): String

    case class Circle(radius: Double, c: String) extends Drawable, Colorable:
      def draw(): String =
        s"circle($radius)"
      def color(): String =
        c
    case class Square(size: Double, c: String) extends Drawable, Colorable:
      def draw(): String =
        s"square($size)"
      def color(): String =
        c

    given showCircle: Show[Circle] = (c: Circle) => s"${c.draw()}:${c.color()}"
    given showSquare: Show[Square] = (s: Square) => s"${s.draw()}:${s.color()}"
    given showDrawable: Show[Drawable] = (d: Drawable) => d.draw()
    given showColorable: Show[Colorable] = (c: Colorable) => c.color()

    type DrawableUnion = Circle | Square
    val s = UnionDeriver.deriveContravariant[Show, DrawableUnion]
    assertEquals(s.show(Circle(5.0, "red")), "circle(5.0):red")
    assertEquals(s.show(Square(3.0, "blue")), "square(3.0):blue")
