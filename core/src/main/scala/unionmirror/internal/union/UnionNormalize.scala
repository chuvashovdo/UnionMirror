package unionmirror.internal.union

import scala.quoted.*

private[unionmirror] object UnionNormalize:

  def normalizeElements(using Quotes)(tpe: quotes.reflect.TypeRepr): List[quotes.reflect.TypeRepr] =
    val raw = UnionFlatten.flattenOr(tpe)
    UnionSort.topoSortBySubtypeThenName(UnionKeys.distinctStable(raw))
