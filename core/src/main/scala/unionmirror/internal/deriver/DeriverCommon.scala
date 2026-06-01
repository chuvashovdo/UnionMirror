package unionmirror.internal.deriver

import scala.quoted.*

import unionmirror.internal.union.UnionNormalize

@SuppressWarnings(Array("org.wartremover.warts.Any"))
private[unionmirror] object DeriverCommon:
  def getElems[T: Type](
    using
    Quotes
  ): List[quotes.reflect.TypeRepr] =

    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias

    root match
      case _: OrType => UnionNormalize.normalizeElements(root)
      case _ => report.errorAndAbort(s"Type ${root.show} is not a Union type")
