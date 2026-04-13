package unionmirror.internal.union

import scala.quoted.*

private[unionmirror] object UnionFlatten:

  def flattenOr(using Quotes)(tpe: quotes.reflect.TypeRepr): List[quotes.reflect.TypeRepr] =
    import quotes.reflect.*
    tpe.dealias match
      case OrType(left, right) => flattenOr(left) ::: flattenOr(right)
      case other => other :: Nil
