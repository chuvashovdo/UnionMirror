package unionmirror.internal.deriver

import scala.annotation.experimental
import scala.quoted.*
import unionmirror.internal.deriver.{ DeriverCommon, DeriverInstanceSummoning, DeriverSamAnalysis }

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.Throw",
  )
)
private[unionmirror] object CovariantSamImpl:
  @experimental
  def covariantSamImpl[F[_]: Type, T: Type](
    using
    Quotes
  ): Expr[F[T]] =
    import quotes.reflect.*

    val elems = DeriverCommon.getElems[T]
    val (samName, argName, argTpe, resTpe) = DeriverSamAnalysis.analyzeSam[F, T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)

    val ftpe: TypeRepr = TypeRepr.of[F[T]]

    @SuppressWarnings(Array("unchecked"))
    def isSafeType(tpe: TypeRepr): Boolean =
      tpe match
        case AppliedType(_, _) =>
          tpe.typeSymbol.fullName match
            case "scala.util.Try" => true
            case "scala.util.Either" => true
            case "scala.Option" => true
            case _ => false
        case _ => false

    val isSafeReturnType = isSafeType(resTpe)

    val clsSym =
      Symbol.newClass(
        Symbol.spliceOwner,
        "$UnionDeriverCovariantSam",
        parents = List(TypeRepr.of[Object], ftpe),
        decls =
          cls =>
            List(
              Symbol.newVal(
                cls,
                "instances",
                TypeRepr.of[Array[Any]],
                Flags.Private,
                Symbol.noSymbol,
              ),
              Symbol.newMethod(
                cls,
                samName,
                MethodType(List(argName))(_ => List(argTpe), _ => resTpe),
                Flags.Override,
                Symbol.noSymbol,
              ),
            ),
        selfType = None,
      )

    val instValSym = clsSym.declaredField("instances")
    val instValDef = ValDef(instValSym, Some(instancesExpr.asTerm))

    val methodSym = clsSym.declaredMethod(samName).head
    val methodDef =
      DefDef(
        methodSym,
        {
          case List(List(arg: Term)) =>
            def makeFallback(idx: Int): Term =
              if idx >= elems.size then
                val msg = s"No instance of ${ftpe.show} could handle the input"
                '{ throw new RuntimeException(${ Expr(msg) }) }.asTerm
              else
                val instAt =
                  Apply(Select.unique(Ref(instValSym), "apply"), List(Literal(IntConstant(idx))))
                val instTyped = Typed(instAt, Inferred(TypeRepr.of[F].appliedTo(elems(idx))))
                val call = Apply(Select.unique(instTyped, samName), List(arg))

                if idx == elems.size - 1 then call
                else if isSafeReturnType then
                  Match(
                    call,
                    List(
                      CaseDef(
                        Typed(Wildcard(), TypeTree.of[scala.util.Right[?, ?]]),
                        None,
                        call,
                      ),
                      CaseDef(
                        Typed(Wildcard(), TypeTree.of[scala.Some[?]]),
                        None,
                        call,
                      ),
                      CaseDef(
                        Typed(Wildcard(), TypeTree.of[scala.util.Success[?]]),
                        None,
                        call,
                      ),
                      CaseDef(
                        Wildcard(),
                        None,
                        makeFallback(idx + 1),
                      ),
                    ),
                  )
                else
                  Try(
                    call,
                    List(
                      CaseDef(
                        Typed(Wildcard(), TypeTree.of[Throwable]),
                        None,
                        makeFallback(idx + 1),
                      )
                    ),
                    None,
                  )

            Some(makeFallback(0))
          case _ => None
        },
      )

    val clsDef =
      ClassDef(
        clsSym,
        parents = List(TypeTree.of[Object], TypeTree.of(using ftpe.asType)),
        body = List(instValDef, methodDef),
      )

    val newCls =
      Typed(
        Apply(Select(New(TypeIdent(clsSym)), clsSym.primaryConstructor), Nil),
        Inferred(ftpe),
      )

    Block(List(clsDef), newCls).asExprOf[F[T]]
