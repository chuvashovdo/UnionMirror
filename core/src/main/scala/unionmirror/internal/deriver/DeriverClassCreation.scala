package unionmirror.internal.deriver

import scala.annotation.experimental
import scala.deriving.Mirror

import scala.quoted.*

@SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
private[unionmirror] object DeriverClassCreation:
  @experimental
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
            val ord = Apply(Select.unique(m.asTerm, "ordinal"), List(arg))
            val instAt = Apply(Select.unique(Ref(instValSym), "apply"), List(ord))
            val instTyped = Typed(instAt, Inferred(ftpe))
            val call = Apply(Select.unique(instTyped, samName), List(arg))
            Some(call)
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
