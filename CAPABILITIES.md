# UnionMirror: Capabilities and Limitations

## Overview

**UnionMirror** is a Scala 3 library that provides type-class derivation mechanisms for union types (`A | B | C`) via synthesized `Mirror.SumOf`. The library enables automatic creation of type-class instances for union types based on instances for each constituent type.

## What you can do with the library

### 1. Type-class derivation for union types

The library supports three primary derivation strategies:

#### 1.1 Contravariant derivation

Suitable for type-classes of the form `F[-T]` (e.g. `Show`, `Encoder`, `Logger`).

**Works automatically for SAM type-classes:**

```scala
trait Printer[-T]:
  def print(value: T): String

given Printer[Int] = (value: Int) => s"int:$value"
given Printer[String] = (value: String) => s"str:$value"

val p = UnionDeriver.deriveContravariant[Printer, Int | String]
p.print(42) // "int:42"
p.print("hello") // "str:hello"
```

**Custom builder support:**

```scala
given UnionDeriver.ContravariantInstanceBuilder[Logger] =
  new UnionDeriver.ContravariantInstanceBuilder[Logger]:
    def build[T](dispatch: T => Logger[Any]): Logger[T] = ...
```

#### 1.2 Covariant derivation

Suitable for type-classes of the form `F[+T]` (e.g. `Parser`, `Decoder`, `Factory`).

**Automatic derivation for SAM with fallback strategy:**

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Int] = (s: String) => s.toInt
given Parser[Boolean] = (s: String) => s.toBoolean

val p = UnionDeriver.deriveCovariant[Parser, Int | Boolean]
p.parse("123") // 123
p.parse("true") // true
// Fallback: if one parser fails, the next one is tried
```

**Custom builder support for complex type-classes:**

```scala
given UnionDeriver.CovariantInstanceBuilder[Decoder] =
  new UnionDeriver.CovariantInstanceBuilder[Decoder]:
    def build[T](elems: IndexedSeq[Decoder[Any]]): Decoder[T] = ...
```

#### 1.3 Binary derivation

Suitable for type-classes with binary operations (e.g. `Eq`, `Order`, `Hash`).

```scala
given UnionDeriver.BinaryInstanceBuilder[Eq] =
  new UnionDeriver.BinaryInstanceBuilder[Eq]:
    def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
      Eq.instance { (x, y) =>
        val ox = ordinal(x)
        val oy = ordinal(y)
        ox == oy && elems(ox).eqv(x, y)
      }

val eq = UnionDeriver.deriveBinary[Eq, Int | String]
eq.eqv(1, 1) // true
eq.eqv(1, "1") // false (different types)
```

### 2. Working with complex types

#### 2.1 Singleton and literal types

```scala
type LiteralUnion = 1 | "a" | true
```

#### 2.2 Object types

```scala
type ObjectUnion = MyObject1.type | MyObject2.type
```

#### 2.3 Parametrized types

```scala
type ParamUnion = List[Int] | Option[String] | Vector[Int]
```

#### 2.4 Multi-parameter types

```scala
type MultiParamUnion = Either[String, Int] | (Int, String) | Map[String, Int]
```

#### 2.5 Parametrized traits

```scala
trait Container[+A]:
  def value: A

type ContUnion = IntContainer | StringContainer | BooleanContainer
```

### 3. Type hierarchies and LSP

The library handles type hierarchies correctly, picking the most specific instance:

```scala
trait Shape:
  def area: Double
case class Circle(radius: Double) extends Shape

given Show[Circle] = (c: Circle) => s"Circle(r=${c.radius})"
given Show[Shape] = (s: Shape) => s"Shape(area=${s.area})"

// For Circle, Show[Circle] is selected, not Show[Shape]
```

Supported:

- Multi-level trait hierarchies
- Sealed traits and their descendants
- Types with shared methods (LSP check)
- Nested unions with traits

### 4. Recursive structures

```scala
case class Leaf(value: Int)
case class Node(left: Tree, right: Tree)
type Tree = Leaf | Node

given Show[Tree] = UnionDeriver.deriveContravariant[Show, Tree]
```

### 5. Union normalization and ordering

The library automatically:

- Flattens nested unions: `Int | (String | Boolean)` → `Int | String | Boolean`
- Removes duplicates: `Int | Int | String` → `Int | String`
- Sorts types for ordinal stability
- Ensures stable ordinals across invocations

### 6. Integration with the Scala ecosystem

#### 6.1 Cats

Supported type-classes:

- `Show` (contravariant)
- `Eq` (binary)
- `Order` (binary)
- `Hash` (binary)

```scala
import unionmirror.interop.cats.instances.given

val show = summon[cats.Show[Cat | Dog]]
val eq = summon[cats.Eq[Cat | Dog]]
val order = summon[cats.Order[Cat | Dog]]
```

#### 6.2 Circe

Supported type-classes:

- `Encoder` (contravariant) — automatic derivation
- `Decoder` (covariant) — automatic derivation via builder

```scala
import unionmirror.interop.circe.instances.given

val encoder = summon[io.circe.Encoder[Cat | Dog]]
val decoder = summon[io.circe.Decoder[Cat | Dog]]
```

**Note:** To avoid conflicts with Circe macros for sealed traits with identical JSON structures, you can use explicit derivation:

```scala
given m: Mirror.SumOf[T] = UnionMirror.synth[T]
val decoder = UnionDeriver.deriveCovariant[Decoder, T]
```

#### 6.3 ZIO Prelude

Supported type-classes:

- `Equal` (binary)
- `Hash` (binary)

```scala
import unionmirror.interop.zio.instances.given
import zio.prelude.*

given Equal[Int] = Equal.make(_ == _)
given Equal[String] = Equal.make(_ == _)

val eq = UnionDeriver.derive[Equal, Int | String]
eq.equal(1, 1) // true
eq.equal(1, "1") // false (different types)
```

### 7. Interoperability with the standard Mirror

The library can work in tandem with the standard Scala 3 `Mirror`:

```scala
enum MyEnum:
  case A, B

type MixedUnion = MyEnum | Cat | Dog
given m: scala.deriving.Mirror.SumOf[MixedUnion] = UnionMirror.synth[MixedUnion]
```

Supported:

- Scala 3 `enum`
- Sealed traits
- Mixing built-in and synthesized mirrors

### 8. Automatic Mirror.SumOf synthesis

With `import unionmirror.auto.given`, the library provides an automatic `given Mirror.SumOf[T]` for union types. This lets you use `summon[Mirror.SumOf[T]]` or receive it via implicit parameters without explicitly calling `UnionMirror.synth[T]`.

```scala
import unionmirror.auto.given

type MyUnion = Int | String | Boolean

// Works automatically via summon
val mirror = summon[Mirror.SumOf[MyUnion]]

// Or via implicit parameters
def process[T](using m: Mirror.SumOf[T]): Unit = ???
process[Int | String] // Works automatically
```

### 9. Scalability

Tests confirm stable behavior with unions of 15+ types.

### 10. Adapter derivation

You can build your own derivation functions on top of `UnionDeriver`:

```scala
inline def deriveEq[T](
  using m: scala.deriving.Mirror.SumOf[T],
  builder: UnionDeriver.BinaryInstanceBuilder[MyEq]
): MyEq[T] =
  UnionDeriver.deriveBinary[MyEq, T]
```

### 11. Optimized fallback strategy for covariant derivation

Automatic covariant SAM derivation detects safe-result types (`Try`, `Either`, `Option`) and uses a more efficient folding strategy instead of try/catch. This provides zero-cost dispatch for type-classes that already use functional error-handling.

For type-classes with unsafe return types, a try/catch fallback is used: if the current parser throws, the next one is tried.

### 12. Performance benchmarks

The `bench` project contains JMH benchmarks for systematic performance measurement:

- SAM type-class derivation time (contravariant and covariant)
- Comparison of try/catch vs folding strategies for covariant derivation
- Runtime operation cost
- Scalability on large union types (5–10 types)
- Performance of binary derivation via builders

Run:

```bash
sbt bench/jmh:run
```

**Benchmark results:**

- SAM type-class derivation: **1–3 ns** (effectively instant)
- Binary type-class derivation: **25–30 ns**
- Large-union derivation (10 types): **3 ns** (no degradation)
- Runtime execution: **2–22 ns**

See [`bench/BENCHMARK_RESULTS.md`](../bench/BENCHMARK_RESULTS.md) for detailed results.

**Conclusion:** The library imposes no noticeable load on the compiler and has excellent runtime performance.

## Limitations and known issues

### 1. Type-level limitations

**Not supported:**

- Unions with higher-kinded parameters (e.g. `List[_] | Option[_]`)
- Unions with path-dependent types
- Unions containing a top type (`Any`, `AnyRef`/`Object`, `Matchable`). Such a union is equivalent to that top type (`Int | String | Any =:= Any`), so a `Mirror.SumOf` cannot soundly distinguish its members. The macro rejects it with a compile error; derive an instance for the top type directly if you need total coverage.

**Erasure collisions for parametrized types (a fundamental JVM limitation):**

For unions like `List[Int] | List[String]`, `Option[Int] | Option[String]`, `Either[A, B] | Either[C, D]` etc., the macro correctly builds a `Mirror.SumOf` with two distinct elements (visible through `MirroredElemLabels`/`MirroredElemTypes`), but **at runtime** `mirror.ordinal(x)` cannot tell them apart due to JVM type erasure. The first matching branch is always selected.

The macro emits a `warning` when synthesizing such a mirror. If runtime discrimination is needed, use wrappers:

```scala
final case class IntList(value: List[Int])
final case class StrList(value: List[String])
type Union = IntList | StrList  // ordinals work correctly
```

For singleton literal types (`1 | 2 | "a"`) there is no problem: Scala 3 compiles `case _: 1` into an equality check.

**Partially supported:**

- **Refinement types** (e.g. `{ def foo: Int } | String`): The library automatically extracts the base type from a refinement and uses it for derivation. For example, `Int { def foo: Int }` is treated as `Int`. This makes refinement-typed unions usable, but the refinement information is lost at runtime.

### 2. No automatic derivation for non-SAM type-classes

**Issue:** Implementing full multi-method derivation runs into difficulties with Scala 3 macros.

**Current status:**

- For SAM type-classes (a single abstract method), automatic derivation works correctly
- For type-classes with multiple methods, an explicit builder is required (`ContravariantInstanceBuilder`, `CovariantInstanceBuilder`, or `BinaryInstanceBuilder`)

**Workaround:** Implement the corresponding builder for the complex type-class.

**Fundamental limitation:** This is a limitation of the current architecture and requires further research and development. Scala 3 macros do not provide convenient mechanisms for automatic analysis and combination of multiple type-class methods.

### 3. Missing integrations for FS2 and others

**✅ Partially addressed:** A ZIO Prelude integration module has been added with `Equal` and `Hash` support.

**Planned:** Additional interop modules (fs2 and other libraries) in future releases.

## Test coverage

Tests are organized into separate files by category:

- **SamTests.scala** — SAM derivation (contravariant and covariant)
- **BinaryInstanceTests.scala** — Binary operations (Eq, Hash) and adapter derivation
- **BuilderTests.scala** — Custom builders
- **AdvancedTypeTests.scala** — Advanced types (singleton, object, parametrized, multi-param, Any, LSP)
- **UnionNormalizationTests.scala** — Union normalization and ordering
- **HierarchyTests.scala** — Trait and class hierarchies
- **interop/cats/CatsInteropTests.scala** — Cats integration
- **interop/circe/CirceInteropTests.scala** — Circe integration
- **interop/zio/ZioInteropTests.scala** — ZIO Prelude integration
- **AutoDeriveTests.scala** — `UnionDeriver.derive` universal API
- **LargeUnionTests.scala** — Large unions and fallback performance
- **MirrorInteropTests.scala** — Interoperability with the standard Mirror
- **RecursiveTests.scala** — Recursive structures

### Performance benchmarks

- **SamDerivationBench.scala** — SAM derivation performance
- **BinaryDerivationBench.scala** — Binary derivation performance
- **LargeUnionBench.scala** — Scalability on large unions

In total: **30+ test scenarios** and **3 benchmark suites** covering the library's main use cases and performance characteristics.

## Conclusion

**UnionMirror** provides a powerful and flexible mechanism for type-class derivation over union types in Scala 3. The library is especially useful for:

- Designing APIs based on union types instead of hierarchies
- Integrating with popular libraries (Cats, Circe, ZIO Prelude)
- Working with complex types (parametrized, recursive, hierarchical)
- Scenarios that require dynamic dispatch by type
- Measuring performance via JMH benchmarks

**Main limitations:**

- Automatic derivation only works for SAM type-classes (a single abstract method)
- Type-classes with multiple methods require an explicit builder
- Some type-level restrictions (higher-kinded parameters, path-dependent types, refinement types)

For the vast majority of practical scenarios the library is fully functional and stable.
