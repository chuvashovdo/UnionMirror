package unionmirror

import scala.annotation.experimental

@experimental final class HierarchyTests extends munit.FunSuite:

  test("hierarchy union: Show[Shape | Circle]"):
    trait Shape:
      def area: Double
    case class Circle(radius: Double) extends Shape:
      def area: Double =
        Math.PI * radius * radius
    case class Rectangle(width: Double, height: Double) extends Shape:
      def area: Double =
        width * height

    trait Show[-T]:
      def show(t: T): String

    given Show[Circle] = (c: Circle) => s"Circle(r=${c.radius})"
    given Show[Shape] = (s: Shape) => s"Shape(area=${s.area})"

    given m: scala.deriving.Mirror.SumOf[Shape | Circle] = UnionMirror.synth[Shape | Circle]
    val s = UnionDeriver.deriveContravariant[Show, Shape | Circle]
    val c = Circle(5.0)

    assertEquals(s.show(c), "Circle(r=5.0)")
    assertEquals(s.show(Rectangle(2.0, 3.0): Shape), "Shape(area=6.0)")

  test("sealed trait union: Show[Shape with Circle | Rectangle]"):
    trait Show[-T]:
      def show(t: T): String

    sealed trait Shape:
      def area: Double
    case class Circle(radius: Double) extends Shape:
      def area: Double =
        Math.PI * radius * radius
    case class Rectangle(width: Double, height: Double) extends Shape:
      def area: Double =
        width * height

    given showCircle: Show[Circle] = (c: Circle) => s"Circle(r=${c.radius})"
    given showRectangle: Show[Rectangle] = (r: Rectangle) => s"Rect(${r.width}x${r.height})"
    @scala.annotation.unused given showShape: Show[Shape] = (s: Shape) => s"Shape(area=${s.area})"

    given m: scala.deriving.Mirror.SumOf[Circle | Rectangle] = UnionMirror.synth[Circle | Rectangle]
    val s = UnionDeriver.deriveContravariant[Show, Circle | Rectangle]
    assertEquals(s.show(Circle(5.0)), "Circle(r=5.0)")
    assertEquals(s.show(Rectangle(2.0, 3.0)), "Rect(2.0x3.0)")

  test("complex trait hierarchy: Show[Animal | (Mammal | (Dog | Cat))]"):
    trait Show[-T]:
      def show(t: T): String

    trait Animal:
      def name: String
    trait Mammal extends Animal:
      def furColor: String
    case class Dog(name: String, furColor: String) extends Mammal
    case class Cat(name: String, furColor: String) extends Mammal
    case class Bird(name: String, canFly: Boolean) extends Animal

    given Show[Dog] = (d: Dog) => s"Dog(${d.name},${d.furColor})"
    given Show[Cat] = (c: Cat) => s"Cat(${c.name},${c.furColor})"
    given Show[Bird] = (b: Bird) => s"Bird(${b.name},${b.canFly})"
    @scala.annotation.unused given Show[Mammal] = (m: Mammal) => s"Mammal(${m.name},${m.furColor})"
    @scala.annotation.unused given Show[Animal] = (a: Animal) => s"Animal(${a.name})"

    given m: scala.deriving.Mirror.SumOf[Dog | Cat | Bird] = UnionMirror.synth[Dog | Cat | Bird]
    val s = UnionDeriver.deriveContravariant[Show, Dog | Cat | Bird]
    assertEquals(s.show(Dog("Rex", "brown")), "Dog(Rex,brown)")
    assertEquals(s.show(Cat("Whiskers", "white")), "Cat(Whiskers,white)")
    assertEquals(s.show(Bird("Tweety", true)), "Bird(Tweety,true)")

  test("nested union with traits: Show[Animal | (Plant | (Tree | Flower))]"):
    trait Show[-T]:
      def show(t: T): String

    trait Living:
      def isAlive: Boolean

    trait Animal extends Living:
      def species: String
    trait Plant extends Living:
      def photosynthesis: Boolean

    case class Dog(species: String) extends Animal:
      def isAlive: Boolean =
        true
    case class Cat(species: String) extends Animal:
      def isAlive: Boolean =
        true
    case class Tree(photosynthesis: Boolean) extends Plant:
      def isAlive: Boolean =
        true
    case class Flower(photosynthesis: Boolean) extends Plant:
      def isAlive: Boolean =
        true

    given showDog: Show[Dog] = (d: Dog) => s"Dog(${d.species})"
    given showCat: Show[Cat] = (c: Cat) => s"Cat(${c.species})"
    given showTree: Show[Tree] = (t: Tree) => s"Tree(${t.photosynthesis})"
    given showFlower: Show[Flower] = (f: Flower) => s"Flower(${f.photosynthesis})"
    @scala.annotation.unused given showAnimal: Show[Animal] = (a: Animal) => s"Animal(${a.species})"
    @scala.annotation.unused given showPlant: Show[Plant] = (p: Plant) => s"Plant(${p.photosynthesis})"
    @scala.annotation.unused given showLiving: Show[Living] = (l: Living) => s"Living(${l.isAlive})"

    type ComplexUnion = Dog | Cat | Tree | Flower
    given m: scala.deriving.Mirror.SumOf[ComplexUnion] = UnionMirror.synth[ComplexUnion]
    val s = UnionDeriver.deriveContravariant[Show, ComplexUnion]
    assertEquals(s.show(Dog("Canis")), "Dog(Canis)")
    assertEquals(s.show(Cat("Felis")), "Cat(Felis)")
    assertEquals(s.show(Tree(true)), "Tree(true)")
    assertEquals(s.show(Flower(false)), "Flower(false)")
