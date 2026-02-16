package unionmirror.internal.union

import scala.quoted.*

private[unionmirror] object UnionKeys:

  def stableKey(using Quotes)(t: quotes.reflect.TypeRepr): String =
    val sym = t.typeSymbol
    if sym.eq(quotes.reflect.Symbol.noSymbol) then t.show else sym.fullName

  def distinctStable(using Quotes)(ts: List[quotes.reflect.TypeRepr]): List[quotes.reflect.TypeRepr] =
    ts.foldLeft(List.empty[quotes.reflect.TypeRepr]) { (acc, t) =>
      if acc.exists(x => stableKey(x) == stableKey(t)) then acc else acc :+ t
    }
