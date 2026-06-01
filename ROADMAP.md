# Roadmap

## Current project status

**Version:** 1.0.0

**Status:** ✅ Core functionality is implemented and tested

### Implemented:

- ✅ Contravariant derivation (SAM + Builder)
- ✅ Covariant derivation (SAM + Builder, with safe-type fallback for `Try`/`Either`/`Option`)
- ✅ Binary derivation (Builder)
- ✅ cats interop (Show, Eq, Order, Hash)
- ✅ circe interop (Encoder, Decoder with error aggregation)
- ✅ ZIO Prelude interop (Equal, Hash)
- ✅ Automatic `Mirror.SumOf` synthesis via `unionmirror.auto.given`
- ✅ Universal `UnionDeriver.derive` with builder-based dispatch
- ✅ Correct deduplication of parametrized types (`List[Int] | List[String]`)
- ✅ JMH benchmarks (see `bench/BENCHMARK_RESULTS.md`)
- ✅ Tests reorganized into per-category files (40+ scenarios)
- ✅ Capability and limitation documentation (CAPABILITIES.md)

**See also:** [CAPABILITIES.md](CAPABILITIES.md) — a detailed description of the library's capabilities, limitations and known issues.

### Plans:

- 📋 Scala 3.4+ LTS support (current version: 3.8.1)
- 📋 Additional interop modules (fs2, scodec)
- 📋 Artifact publishing (Maven Central / Sonatype)
- 📋 Research into multi-method derivation without builders

## Completed tasks

### ✅ deriveCovariant (producing type-classes)

**Status:** Fully implemented (builder + SAM)

**Description:**

- Added `CovariantInstanceBuilder[F[_]]` to `UnionDeriver`
- Implemented `CovariantWithBuilderImpl` for type-classes that take a list of instances
- Implemented `CovariantSamImpl` for automatic derivation of covariant SAM type-classes
- Fallback strategy: sequential iteration over instances with try/catch until the first success

**Usage example (circe — builder):**

```scala
given UnionDeriver.CovariantInstanceBuilder[Decoder] =
  new UnionDeriver.CovariantInstanceBuilder[Decoder]:
    def build[T](elems: IndexedSeq[Decoder[Any]]): Decoder[T] =
      Decoder.instance { (c: io.circe.HCursor) =>
        elems.foldLeft[Decoder.Result[T]](...) { (acc, decoder) =>
          // fallback: try each decoder in turn
        }
      }

inline given [T](using Mirror.SumOf[T]): Decoder[T] =
  UnionDeriver.deriveCovariant[Decoder, T]
```

**Usage example (SAM — automatic derivation):**

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Cat] = (s: String) => /* parsing Cat */
given Parser[Dog] = (s: String) => /* parsing Dog */

val p = UnionDeriver.deriveCovariant[Parser, Cat | Dog]
// Automatically generates a class with fallback logic
```

**Limitations:**

- For non-SAM type-classes, an explicit `CovariantInstanceBuilder` is required
- The fallback uses try/catch, which can be slow in high-load scenarios

### ✅ deriveBinary (binary operations)

**Status:** Implemented

**Description:**

- Added `BinaryInstanceBuilder[F[_]]` to `UnionDeriver`
- Implemented `BinaryWithBuilderImpl` for type-classes with binary operations
- Logic: compare ordinals first, then delegate to the concrete-type instance

**Usage example (cats):**

```scala
// Eq
given UnionDeriver.BinaryInstanceBuilder[Eq] =
  new UnionDeriver.BinaryInstanceBuilder[Eq]:
    def build[T](ordinal: T => Int, elems: IndexedSeq[Eq[Any]]): Eq[T] =
      new Eq[T]:
        def eqv(x: T, y: T): Boolean =
          val ox = ordinal(x)
          val oy = ordinal(y)
          if ox != oy then false
          else elems(ox).eqv(x, y)

// Order
given UnionDeriver.BinaryInstanceBuilder[Order] =
  new UnionDeriver.BinaryInstanceBuilder[Order]:
    def build[T](ordinal: T => Int, elems: IndexedSeq[Order[Any]]): Order[T] =
      new Order[T]:
        def compare(x: T, y: T): Int =
          val ox = ordinal(x)
          val oy = ordinal(y)
          if ox != oy then Integer.compare(ox, oy)
          else elems(ox).compare(x, y)
```

**Tests:**

- `Eq[Cat | Dog]` — values of different types are not equal; values of the same type delegate to the underlying instance
- `Order[Cat | Dog]` — different types are compared by ordinal; equal types delegate

## Completed research

### ✅ Performance

See `bench/BENCHMARK_RESULTS.md`. JMH benchmarks cover:

- SAM type-class derivation (contravariant and covariant)
- try/catch vs. folding comparison for covariant SAM
- Binary derivation via builders
- Scalability on large unions (5–10 types)

Since 0.2.0 the instance array is allocated once per derivation (previously — on each SAM-method invocation).

## Project layout

```
core/
├── UnionDeriver.scala               # Public derivation API
├── UnionMirror.scala                # Mirror.SumOf synthesis
├── auto/package.scala               # Automatic given Mirror.SumOf
└── internal/
    ├── UnionDeriverImpl.scala       # Splice wrappers for macro impls
    ├── UnionMirrorImpl.scala        # Mirror.SumOf implementation
    ├── TupleTypeBuilder.scala       # Tuple-type construction from List[TypeRepr]
    ├── deriver/
    │   ├── AutoSamImpl.scala        # derive[F,T] auto SAM dispatch
    │   ├── ContravariantWithBuilderImpl.scala
    │   ├── ContravariantSamImpl.scala
    │   ├── CovariantWithBuilderImpl.scala
    │   ├── CovariantSamImpl.scala   # SAM with safe-type fallback
    │   ├── BinaryWithBuilderImpl.scala
    │   ├── DeriverCommon.scala
    │   ├── DeriverInstanceSummoning.scala
    │   ├── DeriverSamAnalysis.scala
    │   └── DeriverClassCreation.scala
    └── union/
        ├── UnionNormalize.scala
        ├── UnionFlatten.scala
        ├── UnionKeys.scala
        └── UnionSort.scala

interop-cats/
└── instances.scala                  # Show, Eq, Order, Hash

interop-circe/
└── instances.scala                  # Encoder, Decoder (builder with error aggregation)

interop-zio/
└── instances.scala                  # Equal, Hash

tests/
├── SamTests.scala
├── AutoDeriveTests.scala
├── BinaryInstanceTests.scala
├── BuilderTests.scala
├── AdvancedTypeTests.scala
├── UnionNormalizationTests.scala
├── HierarchyTests.scala
├── LargeUnionTests.scala
├── MirrorInteropTests.scala
├── RecursiveTests.scala
├── CommonModels.scala
└── interop/{cats,circe,zio}/...InteropTests.scala
```

## Usage examples

### Contravariant (Show, Encoder)

```scala
given Show[Cat] = Show.show(c => s"cat:${c.name}")
given Show[Dog] = Show.show(d => s"dog:${d.name}")

val s = summon[Show[Cat | Dog]]
s.show(Cat("m")) // "cat:m"
```

### Binary (Eq, Order)

```scala
given Eq[Cat] = Eq.by[Cat, String](_.name)
given Eq[Dog] = Eq.by[Dog, String](_.name)

val eq = summon[Eq[Cat | Dog]]
eq.eqv(Cat("m"), Cat("m")) // true
eq.eqv(Cat("m"), Dog("m")) // false (different types)
```

### Covariant (Decoder) — via builder

```scala
given Decoder[Cat] = ...
given Decoder[Dog] = ...

val dec = summon[Decoder[Cat | Dog]]
// Tries Decoder[Cat], then Decoder[Dog]
```

### Covariant (Parser) — automatic SAM derivation

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Cat] = (s: String) => Cat(s)
given Parser[Dog] = (s: String) => Dog(s)

val p = UnionDeriver.deriveCovariant[Parser, Cat | Dog]
// Automatically generates a class with fallback logic
p.parse("cat:m") // Cat("m")
p.parse("dog:b") // Dog("b")
```

## Test coverage

Tests are reorganized into per-category files (30+ scenarios):

### SamTests.scala (3 tests)

- Contravariant SAM — `Printer[Int | String]`
- Covariant SAM with fallback — `Parser[Int | Boolean]`
- Covariant SAM with custom classes — `Parser[Cat | Dog]`

### BinaryInstanceTests.scala (3 tests)

- Binary type-class via Builder — `Eq[Int | String]`
- Cats Hash interop — `Hash[Int | String | Boolean]`
- Adapter derivation — explicit use of `Mirror.SumOf`

### BuilderTests.scala (2 tests)

- Custom ContravariantInstanceBuilder — `Logger[Int | String | Boolean]`
- Custom CovariantInstanceBuilder — `Factory[Int | String | Boolean]`

### AdvancedTypeTests.scala (8 tests)

- Singleton types — `1 | "a" | true`
- Object types — `MyObject1.type | MyObject2.type`
- Parametrized traits — `Container[Int] | Container[String]`
- Parametrized types — `List[Int] | Option[String] | Vector[Int]`
- Multi-parameter types — `Either[String, Int] | (Int, String) | Map[String, Int]`
- Union with Any — `Int | String | Any`
- LSP with shared methods — `Drawable | (Circle | Square)`

### UnionNormalizationTests.scala (5 tests)

- Nested unions and normalization — `Int | (String | Boolean)`
- Normalization and element ordering
- Ordinal stability across invocations
- Error reporting for non-union types
- Error reporting for non-SAM types without a builder

### HierarchyTests.scala (4 tests)

- Type hierarchy — `Shape | Circle`
- Sealed-trait union — `Shape with Circle | Rectangle`
- Complex trait hierarchy — `Animal | (Mammal | (Dog | Cat))`
- Nested unions with traits — `Animal | (Plant | (Tree | Flower))`

### InteropTests.scala (6 tests)

- Cats Show interop — `Show[Cat] + Show[Dog] => Show[Cat | Dog]`
- Cats Eq interop — `Eq[Cat] + Eq[Dog] => Eq[Cat | Dog]`
- Cats Order interop — `Order[Cat] + Order[Dog] => Order[Cat | Dog]`
- Cats deep interop — `Order[Int | String | Boolean]`
- Circe Decoder interop — `Decoder[LocalCat | LocalDog | Int]`
- Circe Encoder interop — `Encoder[LocalCat | LocalDog | Int]`

### LargeUnionTests.scala (2 tests)

- Large union — 15 types
- Fallback performance with multiple types

### MirrorInteropTests.scala (3 tests)

- Interop with the built-in Mirror (enum) — `MyEnum | (Cat | Dog)`
- Scala 3 enum Mirror interop — `StandardEnum | Int`
- Scala 3 sealed-trait Mirror interop — `StandardSealed | String`

### RecursiveTests.scala (2 tests)

- Recursive union — `Tree = Leaf | Node`
- Adapter derivation with recursive types — `Serialize[RecursiveTree]`
