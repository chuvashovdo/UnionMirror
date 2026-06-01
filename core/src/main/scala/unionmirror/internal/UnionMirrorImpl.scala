package unionmirror.internal

import scala.deriving.Mirror
import scala.quoted.*
import unionmirror.internal.union.UnionKeys
import unionmirror.internal.union.UnionNormalize

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.AsInstanceOf"))
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

        val parametrized =
          elems.collect {
            case t @ AppliedType(ctor, _) => (t, ctor.typeSymbol.fullName)
          }
        parametrized.groupBy(_._2).values.filter(_.size > 1).foreach { group =>
          val names = group.map(_._1.show).mkString(", ")
          report.warning {
            s"Union elements [$names] share the same JVM erasure; " +
              "runtime ordinal dispatch cannot distinguish them and will always select the first match. " +
              "Use distinct wrapper case classes if you need runtime discrimination."
          }
        }

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
