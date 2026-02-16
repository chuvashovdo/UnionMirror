package unionmirror.internal

import scala.deriving.Mirror
import scala.quoted.*

import unionmirror.UnionDeriver
import unionmirror.internal.union.UnionNormalize

private[unionmirror] object UnionDeriverImpl:
  inline def deriveContravariantWithBuilder[F[_], T](
    using
    m: Mirror.SumOf[T],
    b: UnionDeriver.ContravariantInstanceBuilder[F],
  ): F[T] =
    ${ contravariantWithBuilderImpl[F, T]('m, 'b) }

  inline def deriveContravariantSAM[F[_], T](
    using
    m: Mirror.SumOf[T]
  ): F[T] =
    ${ contravariantSamImpl[F, T]('m) }

  def contravariantWithBuilderImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]],
    b: Expr[UnionDeriver.ContravariantInstanceBuilder[F]],
  )(using
    Quotes
  ): Expr[F[T]] =
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias
    val elems: List[TypeRepr] =
      root match
        case _: OrType => UnionNormalize.normalizeElements(root)
        case _ => report.errorAndAbort(s"Type ${root.show} is not a Union type")

    def summonInstance(tpe: TypeRepr): Expr[Any] =
      tpe.asType match
        case '[a] =>
          Expr.summon[F[a]] match
            case Some(inst) => inst.asExprOf[Any]
            case None => report.errorAndAbort(s"Could not summon instance for ${Type.show[F[a]]}")

    val instancesExpr: Expr[Array[Any]] =
      '{
        scala.Array[Any](${ Varargs(elems.map(summonInstance)) }*)
      }

    '{
      val instances = $instancesExpr
      val dispatch: T => F[Any] =
        (t: T) =>
          val ord = $m.ordinal(t)
          instances(ord).asInstanceOf[F[Any]]
      $b.build[T](dispatch)
    }

  def contravariantSamImpl[F[_]: Type, T: Type](
    m: Expr[Mirror.SumOf[T]]
  )(using
    Quotes
  ): Expr[F[T]] =
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias

    val elems: List[TypeRepr] =
      root match
        case _: OrType => UnionNormalize.normalizeElements(root)
        case _ => report.errorAndAbort(s"Type ${root.show} is not a Union type")

    val ftpe: TypeRepr = TypeRepr.of[F[T]]
    val fCtorSym = TypeRepr.of[F].classSymbol.getOrElse(TypeRepr.of[F].typeSymbol)

    val abstractMethods =
      val ms = fCtorSym.methodMembers
      val candidates = if ms.nonEmpty then ms else fCtorSym.declaredMethods
      candidates
        .filter(m => m.flags.is(Flags.Deferred) || m.flags.is(Flags.Abstract))
        .filterNot(_.flags.is(Flags.Synthetic))
        .filterNot(_.name == "<init>")
        .distinct

    if abstractMethods.size != 1 then
      report.errorAndAbort {
        s"${Type.show[F]} is not a SAM typeclass (found ${abstractMethods.size} abstract methods); provide an explicit ContravariantInstanceBuilder"
      }

    val sam0 = abstractMethods.head
    val samName = sam0.name

    val samTpe0 = ftpe.memberType(sam0).widen
    val (argName, argTpe, resTpe) =
      samTpe0 match
        case mt: MethodType =>
          if mt.paramNames.size != 1 then
            report.errorAndAbort(s"SAM method $samName must have exactly one parameter")
          (mt.paramNames.head, mt.paramTypes.head, mt.resType)
        case _ =>
          report.errorAndAbort(s"Unsupported SAM type for ${Type.show[F]}: ${samTpe0.show}")

    if !(TypeRepr.of[T] <:< argTpe) then
      report.errorAndAbort(
        s"SAM method $samName parameter type ${argTpe.show} is not compatible with union type ${Type.show[T]}"
      )

    def summonInstanceExpr(tpe: TypeRepr): Expr[Any] =
      tpe.asType match
        case '[a] =>
          Expr.summon[F[a]] match
            case Some(inst) => inst.asExprOf[Any]
            case None => report.errorAndAbort(s"Could not summon instance for ${Type.show[F[a]]}")

    val instancesExpr: Expr[Array[Any]] =
      '{ scala.Array[Any](${ Varargs(elems.map(summonInstanceExpr)) }*) }

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
