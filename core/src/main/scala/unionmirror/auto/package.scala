package unionmirror.auto

import scala.deriving.Mirror

export unionmirror.UnionMirror.synth

transparent inline given mirrorSumOf[T]: Mirror.SumOf[T] =
  ${ unionmirror.internal.UnionMirrorImpl.derivedUnionMirrorOf[T] }
