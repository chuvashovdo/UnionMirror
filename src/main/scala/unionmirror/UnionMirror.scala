package unionmirror

import scala.deriving.Mirror
import scala.quoted.*
import unionmirror.internal.UnionMirrorImpl

object UnionMirror:
  transparent inline given derived[T]: Mirror.SumOf[T] =
    ${ UnionMirrorImpl.derivedUnionMirrorOf[T] }

  transparent inline def synth[T]: Mirror.SumOf[T] =
    ${ UnionMirrorImpl.derivedUnionMirrorOf[T] }
