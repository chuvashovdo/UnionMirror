package unionmirror.internal.deriver

import scala.quoted.*

@SuppressWarnings(Array("org.wartremover.warts.Any"))
private[unionmirror] object DeriverInstanceSummoning:
  def summonInstances[F[_]: Type](
    using
    Quotes
  )(
    elems: List[quotes.reflect.TypeRepr]
  ): Expr[Array[Any]] =
    import quotes.reflect.*

    def summonInstance(tpe: TypeRepr): Expr[Any] =
      tpe.asType match
        case '[a] =>
          Expr.summon[F[a]] match
            case Some(inst) => inst.asExprOf[Any]
            case None => report.errorAndAbort(s"Could not summon instance for ${Type.show[F[a]]}")

    '{
      scala.Array[Any](${ Varargs(elems.map(summonInstance)) }*)
    }
