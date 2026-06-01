package unionmirror.bench

import org.openjdk.jmh.annotations.*
import unionmirror.UnionDeriver
import unionmirror.auto.given
import scala.compiletime.uninitialized

import scala.deriving.Mirror

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
class SamDerivationBench:
  trait Printer[-T]:
    def print(value: T): String

  given Printer[Int] =
    (value: Int) => s"int:$value"
  given Printer[String] =
    (value: String) => s"str:$value"
  given Printer[Boolean] =
    (value: Boolean) => s"bool:$value"

  trait Parser[+T]:
    def parse(s: String): T

  given Parser[Int] =
    (s: String) => s.toInt
  given Parser[String] =
    (s: String) => s
  given Parser[Boolean] =
    (s: String) => s.toBoolean

  trait SafeParser[+T]:
    def parse(s: String): Option[T]

  given SafeParser[Int] =
    (s: String) => scala.util.Try(s.toInt).toOption
  given SafeParser[String] =
    (s: String) => Some(s)
  given SafeParser[Boolean] =
    (s: String) => scala.util.Try(s.toBoolean).toOption

  @Benchmark
  def deriveContravariantSmall: Printer[Int | String] =
    UnionDeriver.deriveContravariant[Printer, Int | String]

  @Benchmark
  def deriveContravariantMedium: Printer[Int | String | Boolean] =
    UnionDeriver.deriveContravariant[Printer, Int | String | Boolean]

  @Benchmark
  def deriveCovariantSmall: Parser[Int | String] =
    UnionDeriver.deriveCovariant[Parser, Int | String]

  @Benchmark
  def deriveCovariantMedium: Parser[Int | String | Boolean] =
    UnionDeriver.deriveCovariant[Parser, Int | String | Boolean]

  @Benchmark
  def deriveCovariantSafeSmall: SafeParser[Int | String] =
    UnionDeriver.deriveCovariant[SafeParser, Int | String]

  @Benchmark
  def deriveCovariantSafeMedium: SafeParser[Int | String | Boolean] =
    UnionDeriver.deriveCovariant[SafeParser, Int | String | Boolean]

  private var printerSmall: Printer[Int | String] =
    uninitialized
  private var printerMedium: Printer[Int | String | Boolean] =
    uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    printerSmall = UnionDeriver.deriveContravariant[Printer, Int | String]
    printerMedium = UnionDeriver.deriveContravariant[Printer, Int | String | Boolean]

  @Benchmark
  def runtimeContravariantSmall: String =
    printerSmall.print(42)

  @Benchmark
  def runtimeContravariantMedium: String =
    printerMedium.print(true)
