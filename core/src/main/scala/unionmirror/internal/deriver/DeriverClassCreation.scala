package unionmirror.internal.deriver

import scala.deriving.Mirror

import scala.quoted.*

object DeriverClassCreation:
  def createSamClass[F[_]: Type, T: Type](
    using
    Quotes
  )(
    samName: String,
    argName: String,
    argTpe: quotes.reflect.TypeRepr,
    resTpe: quotes.reflect.TypeRepr,
    instancesExpr: Expr[Array[Any]],
    m: Expr[Mirror.SumOf[T]],
  ): Expr[F[T]] =

    import quotes.reflect.*

    val ftpe: TypeRepr = TypeRepr.of[F[T]]

    val clsSym =
      Symbol.newClass(
        Symbol.spliceOwner,
        "$UnionDeriverSam",
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
            val ord = Apply(Select.unique(m.asTerm, "ordinal"), List(arg))
            val instAt = Apply(Select.unique(Ref(instValSym), "apply"), List(ord))
            val instTyped = Typed(instAt, Inferred(ftpe))
            val call = Apply(Select.unique(instTyped, samName), List(arg))
            Some(Block(List(instVal), call))
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
