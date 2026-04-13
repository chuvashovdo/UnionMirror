package unionmirror.internal

import scala.quoted.*

private[unionmirror] object TupleTypeBuilder:
  def makeTupleType(
    using
    Quotes
  )(
    ts: List[quotes.reflect.TypeRepr]
  ): Type[?] =
    ts match
      case Nil => Type.of[EmptyTuple]
      case head :: tail =>
        head.asType match
          case '[ht] =>
            makeTupleType(tail) match
              case '[type tt <: Tuple; tt] => Type.of[ht *: tt]

  def makeLabelsType(
    using
    Quotes
  )(
    labels: List[String]
  ): Type[?] =
    import quotes.reflect.*
    labels match
      case Nil => Type.of[EmptyTuple]
      case head :: tail =>
        val headT = ConstantType(StringConstant(head)).asType
        headT match
          case '[ht] =>
            makeLabelsType(tail) match
              case '[type tt <: Tuple; tt] => Type.of[ht *: tt]
