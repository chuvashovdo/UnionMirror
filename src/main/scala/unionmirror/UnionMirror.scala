package unionmirror

import scala.deriving.Mirror
import scala.quoted.*

object UnionMirror:
  transparent inline given derived[T]: Mirror.SumOf[T] =
    ${ UnionMirrorImpl.derivedUnionMirrorOf[T] }

  transparent inline def synth[T]: Mirror.SumOf[T] =
    ${ UnionMirrorImpl.derivedUnionMirrorOf[T] }

private object UnionMirrorImpl:
  def derivedUnionMirrorOf[T: Type](
    using
    Quotes
  ): Expr[Mirror.SumOf[T]] =
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias

    def flattenOr(tpe: TypeRepr): List[TypeRepr] =
      tpe.dealias match
        case OrType(left, right) => flattenOr(left) ::: flattenOr(right)
        case other => other :: Nil

    def stableKey(t: TypeRepr): String =
      val sym = t.typeSymbol
      if sym.eq(Symbol.noSymbol) then t.show else sym.fullName

    def distinctStable(ts: List[TypeRepr]): List[TypeRepr] =
      ts.foldLeft(List.empty[TypeRepr]) { (acc, t) =>
        if acc.exists(x => stableKey(x) == stableKey(t)) then acc else acc :+ t
      }

    def topoSort(ts: List[TypeRepr]): List[TypeRepr] =
      val keys = ts.map(stableKey)
      val edges =
        ts.indices
          .map { i =>
            i -> ts.indices.filter(j => i != j && (ts(i) <:< ts(j)) && !(ts(j) <:< ts(i))).toSet
          }
          .toMap
      val inDegree = Array.fill(ts.size)(0)
      edges.values.foreach(_.foreach(j => inDegree(j) += 1))
      @annotation.tailrec
      def loop(nodes: List[Int], out: List[Int]): List[Int] =
        if nodes.isEmpty then out
        else
          val next = nodes.minBy(keys)
          val newlyZero =
            edges(next).filter { j =>
              inDegree(j) -= 1
              inDegree(j) == 0
            }
          loop(nodes.filterNot(_ == next) ++ newlyZero.toList, out :+ next)
      loop(ts.indices.filter(inDegree(_) == 0).toList, Nil).map(ts)

    def makeTupleType(ts: List[TypeRepr]): Type[?] =
      ts match
        case Nil => Type.of[EmptyTuple]
        case head :: tail =>
          head.asType match
            case '[ht] =>
              makeTupleType(tail) match
                case '[type tt <: Tuple; tt] => Type.of[ht *: tt]

    def makeLabelsType(labels: List[String]): Type[?] =
      labels match
        case Nil => Type.of[EmptyTuple]
        case head :: tail =>
          val headT = ConstantType(StringConstant(head)).asType
          headT match
            case '[ht] =>
              makeLabelsType(tail) match
                case '[type tt <: Tuple; tt] => Type.of[ht *: tt]

    def ordinalExpr(x: Expr[Any], elems: List[TypeRepr]): Expr[Int] =
      val cases =
        elems.zipWithIndex.map { (tpe, idx) =>
          val pat = Typed(Wildcard(), Inferred(tpe))
          CaseDef(pat, None, Literal(IntConstant(idx)))
        }
      Match(x.asTerm, cases :+ CaseDef(Wildcard(), None, Literal(IntConstant(-1)))).asExprOf[Int]

    root match
      case _: OrType =>
        val raw = flattenOr(root)
        val elems = topoSort(distinctStable(raw))

        val ets = makeTupleType(elems)
        val els = makeLabelsType(elems.map(stableKey))

        (ets, els) match
          case ('[type etT <: Tuple; etT], '[type elT <: Tuple; elT]) =>
            '{
              val m: Mirror.Sum {
                type MirroredType = T
                type MirroredMonoType = T
                type MirroredElemTypes = etT
                type MirroredElemLabels = elT
              } =
                new Mirror.Sum:
                  type MirroredType =
                    T
                  type MirroredMonoType =
                    T
                  type MirroredElemTypes =
                    etT
                  type MirroredElemLabels =
                    elT

                  def ordinal(x: T): Int =
                    ${ ordinalExpr('x, elems) }

              m.asInstanceOf[
                Mirror.SumOf[T] {
                  type MirroredElemTypes = etT
                  type MirroredElemLabels = elT
                }
              ]
            }.asExprOf[Mirror.SumOf[T]]
          case _ =>
            report.errorAndAbort("Could not prove that synthesized types are Tuples")
      case _ =>
        report.errorAndAbort(s"Type ${root.show} is not a Union type")
