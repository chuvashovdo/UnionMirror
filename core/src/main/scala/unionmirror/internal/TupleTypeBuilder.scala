package unionmirror.internal

import scala.quoted.*

private[unionmirror] object TupleTypeBuilder:
  def makeTupleType(
    using
    Quotes
  )(
    ts: List[quotes.reflect.TypeRepr]
  ): quotes.reflect.TypeRepr =
    import quotes.reflect.*
    ts.foldRight(TypeRepr.of[EmptyTuple]) { (head, tail) =>
      TypeRepr.of[*:].appliedTo(List(head, tail))
    }

  def makeLabelsType(
    using
    Quotes
  )(
    labels: List[String]
  ): quotes.reflect.TypeRepr =
    import quotes.reflect.*
    labels.foldRight(TypeRepr.of[EmptyTuple]) { (head, tail) =>
      TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(head)), tail))
    }
