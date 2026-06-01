package unionmirror.internal.union

import scala.quoted.*

private[unionmirror] object UnionKeys:

  def stableKey(using Quotes)(t: quotes.reflect.TypeRepr): String =
    t.show

  def distinctStable(using Quotes)(ts: List[quotes.reflect.TypeRepr]): List[quotes.reflect.TypeRepr] =
    ts.foldLeft(List.empty[quotes.reflect.TypeRepr]) { (acc, t) =>
      if acc.exists(x => x =:= t) then acc else acc :+ t
    }
