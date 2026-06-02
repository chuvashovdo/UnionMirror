package unionmirror.bench

import org.openjdk.jmh.annotations.*
import unionmirror.UnionDeriver
import unionmirror.auto.given
import scala.annotation.experimental
import scala.compiletime.uninitialized

import scala.deriving.Mirror

@experimental
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(java.util.concurrent.TimeUnit.MICROSECONDS)
class LargeUnionBench:
  trait Printer[-T]:
    def print(value: T): String

  given Printer[Int] =
    (value: Int) => s"int:$value"
  given Printer[String] =
    (value: String) => s"str:$value"
  given Printer[Boolean] =
    (value: Boolean) => s"bool:$value"
  given Printer[Long] =
    (value: Long) => s"long:$value"
  given Printer[Double] =
    (value: Double) => s"double:$value"
  given Printer[Float] =
    (value: Float) => s"float:$value"
  given Printer[Short] =
    (value: Short) => s"short:$value"
  given Printer[Byte] =
    (value: Byte) => s"byte:$value"
  given Printer[Char] =
    (value: Char) => s"char:$value"
  given Printer[Unit] =
    (value: Unit) => s"unit:$value"

  @Benchmark
  def deriveUnion5: Printer[Int | String | Boolean | Long | Double] =
    UnionDeriver.deriveContravariant[Printer, Int | String | Boolean | Long | Double]

  @Benchmark
  def deriveUnion10: Printer[Int | String | Boolean | Long | Double | Float | Short | Byte | Char | Unit] =
    UnionDeriver.deriveContravariant[
      Printer,
      Int | String | Boolean | Long | Double | Float | Short | Byte | Char | Unit,
    ]

  private var printer5: Printer[Int | String | Boolean | Long | Double] =
    uninitialized
  private var printer10: Printer[Int | String | Boolean | Long | Double | Float | Short | Byte | Char | Unit] =
    uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    printer5 = UnionDeriver.deriveContravariant[Printer, Int | String | Boolean | Long | Double]
    printer10 =
      UnionDeriver.deriveContravariant[
        Printer,
        Int | String | Boolean | Long | Double | Float | Short | Byte | Char | Unit,
      ]

  @Benchmark
  def runtimeUnion5: String =
    printer5.print(42)

  @Benchmark
  def runtimeUnion10: String =
    printer10.print(42)
