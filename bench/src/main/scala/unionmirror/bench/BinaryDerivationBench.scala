package unionmirror.bench

import org.openjdk.jmh.annotations.*
import unionmirror.UnionDeriver
import unionmirror.auto.given
import cats.Eq
import scala.compiletime.uninitialized

import scala.deriving.Mirror

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
class BinaryDerivationBench:
  given Eq[Int] =
    Eq.fromUniversalEquals[Int]
  given Eq[String] =
    Eq.fromUniversalEquals[String]
  given Eq[Boolean] =
    Eq.fromUniversalEquals[Boolean]

  given UnionDeriver.BinaryInstanceBuilder[Eq] =
    new UnionDeriver.BinaryInstanceBuilder[Eq]:
      def build[T](ordinal: T => Int, elems: List[Eq[Any]]): Eq[T] =
        Eq.instance { (x, y) =>
          val ox = ordinal(x)
          val oy = ordinal(y)
          ox == oy && elems(ox).eqv(x, y)
        }

  @Benchmark
  def deriveBinarySmall: Eq[Int | String] =
    UnionDeriver.deriveBinary[Eq, Int | String]

  @Benchmark
  def deriveBinaryMedium: Eq[Int | String | Boolean] =
    UnionDeriver.deriveBinary[Eq, Int | String | Boolean]

  private var eqSmall: Eq[Int | String] =
    uninitialized
  private var eqMedium: Eq[Int | String | Boolean] =
    uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    eqSmall = UnionDeriver.deriveBinary[Eq, Int | String]
    eqMedium = UnionDeriver.deriveBinary[Eq, Int | String | Boolean]

  @Benchmark
  def runtimeBinarySmall: Boolean =
    eqSmall.eqv(1, 1)

  @Benchmark
  def runtimeBinaryMedium: Boolean =
    eqMedium.eqv(true, true)
