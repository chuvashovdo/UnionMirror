package unionmirror.internal.deriver

import scala.quoted.*

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.IterableOps"))
private[unionmirror] object DeriverSamAnalysis:
  def analyzeSam[F[_]: Type, T: Type](
    using
    Quotes
  ): (String, String, quotes.reflect.TypeRepr, quotes.reflect.TypeRepr) =

    import quotes.reflect.*

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
        s"${Type.show[F]} is not a SAM typeclass (found ${abstractMethods.size} abstract methods); provide an explicit InstanceBuilder (Contravariant/Covariant/Binary)"
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

    (samName, argName, argTpe, resTpe)
