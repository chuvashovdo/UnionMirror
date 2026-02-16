package unionmirror.internal

import scala.deriving.Mirror
import scala.quoted.*
import unionmirror.internal.union.UnionKeys
import unionmirror.internal.union.UnionNormalize

private[unionmirror] object UnionMirrorImpl:
  def derivedUnionMirrorOf[T: Type](
    using
    Quotes
  ): Expr[Mirror.SumOf[T]] =
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias

    def ordinalExpr(x: Expr[Any], elems: List[TypeRepr]): Expr[Int] =
      val cases =
        elems.zipWithIndex.map { (tpe, idx) =>
          val pat = Typed(Wildcard(), Inferred(tpe))
          CaseDef(pat, None, Literal(IntConstant(idx)))
        }
      Match(x.asTerm, cases :+ CaseDef(Wildcard(), None, Literal(IntConstant(-1)))).asExprOf[Int]

    root match
      case _: OrType =>
        val elems = UnionNormalize.normalizeElements(root)

        val ets = TupleTypeBuilder.makeTupleType(elems)
        val els = TupleTypeBuilder.makeLabelsType(elems.map(UnionKeys.stableKey))

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
