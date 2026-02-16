package unionmirror.internal.union

import scala.quoted.*

private[unionmirror] object UnionSort:
  def topoSortBySubtypeThenName(
    using
    Quotes
  )(
    ts: List[quotes.reflect.TypeRepr]
  ): List[quotes.reflect.TypeRepr] =
    import quotes.reflect.*

    val keys = ts.map(UnionKeys.stableKey)

    val edges: Map[Int, Set[Int]] =
      ts.indices
        .map { i =>
          i -> ts
            .indices
            .filter { j =>
              i != j && (ts(i) <:< ts(j)) && !(ts(j) <:< ts(i))
            }
            .toSet
        }
        .toMap

    val inDegree = Array.fill(ts.size)(0)
    edges.values.foreach(_.foreach(j => inDegree(j) += 1))

    def pickMinByKey(nodes: List[Int]): Int =
      nodes match
        case Nil =>
          throw new IllegalArgumentException("pickMinByKey called with empty nodes")
        case head :: tail =>
          tail.foldLeft(head) { (best, cur) =>
            if keys(cur) < keys(best) then cur else best
          }

    @annotation.tailrec
    def loop(nodes: List[Int], out: List[Int]): List[Int] =
      if nodes.isEmpty then out
      else
        val next = pickMinByKey(nodes)
        val newlyZero =
          edges(next).filter { j =>
            inDegree(j) -= 1
            inDegree(j) == 0
          }
        loop(nodes.filterNot(_ == next) ++ newlyZero.toList, out :+ next)

    loop(ts.indices.filter(inDegree(_) == 0).toList, Nil).map(ts)
