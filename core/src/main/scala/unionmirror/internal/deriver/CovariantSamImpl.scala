package unionmirror.internal.deriver

import scala.deriving.Mirror
import scala.quoted.*
import unionmirror.internal.deriver.{
  DeriverCommon,
  DeriverInstanceSummoning,
  DeriverSamAnalysis,
  DeriverClassCreation,
}

object CovariantSamImpl:
  def covariantSamImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]]
  )(using
    Quotes
  ): Expr[F[T]] =
    import quotes.reflect.*

    val elems = DeriverCommon.getElems[T]
    val (samName, argName, argTpe, resTpe) = DeriverSamAnalysis.analyzeSam[F, T]
    val instancesExpr = DeriverInstanceSummoning.summonInstances[F](elems)

    val ftpe: TypeRepr = TypeRepr.of[F[T]]

    val clsSym =
      Symbol.newClass(
        Symbol.spliceOwner,
        "$UnionDeriverCovariantSam",
        parents = List(TypeRepr.of[Object], ftpe),
        decls =
          cls =>
            List(
              Symbol.newMethod(
                cls,
                samName,
                MethodType(List(argName))(_ => List(argTpe), _ => resTpe),
                Flags.Override,
                Symbol.noSymbol,
              )
            ),
        selfType = None,
      )

    val methodSym = clsSym.declaredMethod(samName).head
    val methodDef =
      DefDef(
        methodSym,
        {
          case List(List(arg: Term)) =>
            val instValSym =
              Symbol.newVal(
                methodSym,
                "instances",
                TypeRepr.of[Array[Any]],
                Flags.EmptyFlags,
                Symbol.noSymbol,
              )
            val instVal = ValDef(instValSym, Some(instancesExpr.asTerm))

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

            Some(Block(List(instVal), makeFallback(0)))
          case _ => None
        },
      )

    val clsDef =
      ClassDef(
        clsSym,
        parents = List(TypeTree.of[Object], TypeTree.of(using ftpe.asType)),
        body = List(methodDef),
      )

    val newCls =
      Typed(
        Apply(Select(New(TypeIdent(clsSym)), clsSym.primaryConstructor), Nil),
        Inferred(ftpe),
      )

    Block(List(clsDef), newCls).asExprOf[F[T]]
