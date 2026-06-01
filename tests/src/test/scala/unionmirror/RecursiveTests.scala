package unionmirror

final class RecursiveTests extends munit.FunSuite:
  import unionmirror.auto.given

  test("recursive union: Show[Tree]"):
    trait Show[-T]:
      def show(t: T): String

    case class Leaf(value: Int)
    case class Node(left: Tree, right: Tree)
    type Tree = Leaf | Node

    given Show[Leaf] = (l: Leaf) => s"L(${l.value})"
    given showNode: Show[Node]:
      def show(n: Node): String =
        val s = summon[Show[Tree]]
        s"N(${s.show(n.left)}, ${s.show(n.right)})"

    given showTree: Show[Tree] = UnionDeriver.deriveContravariant[Show, Tree]

    val tree: Tree = Node(Leaf(1), Node(Leaf(2), Leaf(3)))
    assertEquals(showTree.show(tree), "N(L(1), N(L(2), L(3)))")

  test("adapter derivation with recursive types: Serialize[RecursiveTree]"):
    trait Serialize[-T]:
      def serialize(value: T): String

    case class Leaf(value: Int)
    case class Node(left: RecursiveTree, right: RecursiveTree)
    type RecursiveTree = Leaf | Node

    given Serialize[Leaf] = (l: Leaf) => s"L(${l.value})"

    given UnionDeriver.BinaryInstanceBuilder[Serialize] =
      new UnionDeriver.BinaryInstanceBuilder[Serialize]:
        def build[T](ordinal: T => Int, elems: IndexedSeq[Serialize[Any]]): Serialize[T] =
          new Serialize[T]:
            def serialize(value: T): String =
              val ov = ordinal(value)
              elems(ov).serialize(value)

    given m: scala.deriving.Mirror.SumOf[RecursiveTree] = UnionMirror.synth[RecursiveTree]

    lazy val treeSerializer: Serialize[RecursiveTree] =
      UnionDeriver.deriveBinary[Serialize, RecursiveTree]

    given Serialize[Node] =
      (n: Node) => s"N(${treeSerializer.serialize(n.left)},${treeSerializer.serialize(n.right)})"

    val tree: RecursiveTree = Node(Leaf(1), Node(Leaf(2), Leaf(3)))
    assertEquals(treeSerializer.serialize(tree), "N(L(1),N(L(2),L(3)))")
