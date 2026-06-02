# UnionMirror

Scala 3 library for deriving type-class instances over **union types** (`A | B | C`) via synthesized `Mirror.SumOf`.

[![Scala 3](https://img.shields.io/badge/Scala-3.3.7%20LTS-red)](https://www.scala-lang.org/)

> **Experimental APIs.** The automatic SAM derivation methods
> (`UnionDeriver.deriveContravariant`, `deriveCovariant`, `derive`) are marked
> `@experimental`, so call sites must opt in — either annotate the enclosing
> scope with `@scala.annotation.experimental` or compile with the
> `-experimental` flag. `UnionMirror.synth` and the builder-based derivations
> (`deriveBinary`, `*InstanceBuilder`) are stable and need no opt-in.

## Features

- Synthesizes `scala.deriving.Mirror.SumOf[T]` for any union type `T`.
- Derives type-class instances for unions in three flavors:
  - **Contravariant** (`F[-T]`, e.g. `Show`, `Encoder`, `Logger`) — automatic for SAMs, custom via `ContravariantInstanceBuilder`.
  - **Covariant** (`F[+T]`, e.g. `Parser`, `Decoder`) — automatic for SAMs (with safe-result detection for `Try`/`Either`/`Option`), custom via `CovariantInstanceBuilder`.
  - **Binary** (`F[T]` with binary ops, e.g. `Eq`, `Order`, `Hash`) — via `BinaryInstanceBuilder`.
- Union normalization: flattens nested ors, removes duplicates, produces a stable topological/lexicographic order so ordinals don’t depend on syntactic order.
- Plays nicely with Scala 3 `enum` / `sealed trait` `Mirror`s in mixed unions.
- Interop modules for **cats**, **circe**, **zio-prelude**.

## Modules

| Module        | Artifact                 | Provides                                                |
| ------------- | ------------------------ | ------------------------------------------------------- |
| core          | `union-derivation-core`  | `UnionMirror`, `UnionDeriver`, `unionmirror.auto.given` |
| interop-cats  | `union-derivation-cats`  | `Show`, `Eq`, `Order`, `Hash`                           |
| interop-circe | `union-derivation-circe` | `Encoder`, `Decoder`                                    |
| interop-zio   | `union-derivation-zio`   | `Equal`, `Hash`                                         |

## Quick start

```scala
libraryDependencies += "your.org" %% "union-derivation-core" % "1.0.0"
```

> The artifact is not yet published; build locally with `sbt publishLocal`.

### Synthesize a `Mirror.SumOf`

```scala
import scala.deriving.Mirror
import unionmirror.UnionMirror

type MyUnion = Int | String | Boolean
given Mirror.SumOf[MyUnion] = UnionMirror.synth[MyUnion]
```

Or import the automatic `given`:

```scala
import unionmirror.auto.given

summon[Mirror.SumOf[Int | String]]
```

### Derive a SAM type-class

```scala
import unionmirror.{ UnionDeriver, auto }
import unionmirror.auto.given

import scala.annotation.experimental

@experimental // deriveContravariant is @experimental
trait Printer[-T]:
  def print(value: T): String

given Printer[Int]    = i => s"int:$i"
given Printer[String] = s => s"str:$s"

val p = UnionDeriver.deriveContravariant[Printer, Int | String]
p.print(42)      // "int:42"
p.print("hello") // "str:hello"
```

### Covariant SAM (auto fallback)

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Int]     = _.toInt
given Parser[Boolean] = _.toBoolean

val p = UnionDeriver.deriveCovariant[Parser, Int | Boolean]
p.parse("123")  // 123
p.parse("true") // true
```

For SAMs returning `Try`/`Either`/`Option`, the generated dispatcher folds over results instead of relying on `try/catch`.

### Custom builders

```scala
given UnionDeriver.BinaryInstanceBuilder[cats.Eq] =
  new UnionDeriver.BinaryInstanceBuilder[cats.Eq]:
    def build[T](ordinal: T => Int, elems: IndexedSeq[cats.Eq[Any]]): cats.Eq[T] =
      cats.Eq.instance: (x, y) =>
        val ox = ordinal(x); val oy = ordinal(y)
        ox == oy && elems(ox).eqv(x, y)

val eq = UnionDeriver.deriveBinary[cats.Eq, Int | String]
```

### Cats interop

```scala
import unionmirror.auto.given
import unionmirror.interop.cats.instances.given

val show  = summon[cats.Show[Cat | Dog]]
val eq    = summon[cats.Eq[Cat | Dog]]
val order = summon[cats.Order[Cat | Dog]]
val hash  = summon[cats.kernel.Hash[Cat | Dog]]
```

### Circe interop

```scala
import unionmirror.auto.given
import unionmirror.interop.circe.instances.given

val enc = summon[io.circe.Encoder[Cat | Dog]]
val dec = summon[io.circe.Decoder[Cat | Dog]]
```

### ZIO Prelude interop

```scala
import unionmirror.auto.given
import unionmirror.interop.zio.instances.given   // or: unionmirror.interop.zioInterop.given
import zio.prelude.*

val eq = UnionDeriver.derive[Equal, Int | String]
```

## Capabilities & limits

See [CAPABILITIES.md](CAPABILITIES.md) for an in-depth tour and [ROADMAP.md](ROADMAP.md) for the development plan.

Supported:

- Singleton/literal unions (`1 | "a" | true`).
- Parametrized types (`List[Int] | Option[String]`).
- Multi-param types (`Either[A, B] | (A, B) | Map[A, B]`).
- Object types and `enum` / `sealed trait` mirrors mixed into unions.
- Recursive unions and trait hierarchies (LSP picks the most specific instance).

Not supported / partial:

- Higher-kinded params in unions (`List[_] | Option[_]`).
- Path-dependent types.
- Refinement types are accepted but the refinement is stripped (e.g. `Int { def foo: Int }` is treated as `Int`).
- For non-SAM type-classes you must supply the matching `*InstanceBuilder`.
- Unions containing a top type (`Any`, `AnyRef`/`Object`, `Matchable`) are rejected at compile time, since `Int | String | Any =:= Any` and a `Mirror.SumOf` cannot soundly distinguish its members.
- **JVM erasure collisions**: `List[Int] | List[String]` (and similar parametrized unions sharing a type constructor) are preserved at compile time but cannot be distinguished by `mirror.ordinal(...)` at runtime — the macro emits a warning. Use wrapper case classes when runtime discrimination is required. Singleton literal unions (`1 | 2`, `"a" | "b"`) are unaffected.

## Building

```bash
sbt compile
sbt test
sbt bench/Jmh/run        # JMH benchmarks
```

## Project layout

```
core/                 # Public API + macro implementations
interop-cats/         # cats Show/Eq/Order/Hash
interop-circe/        # circe Encoder/Decoder
interop-zio/          # zio-prelude Equal/Hash
bench/                # JMH benchmarks
tests/                # munit test suite
```

## License

[MIT License](LICENSE).
