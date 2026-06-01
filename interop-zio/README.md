# UnionMirror ZIO Prelude Interop

Integration module for [ZIO Prelude](https://zio.dev/zio-prelude/), providing automatic type-class derivation for union types.

## Supported type-classes

- `Equal` — binary derivation
- `Hash` — binary derivation

## Usage

```scala
import unionmirror.interop.zio.instances.given
import unionmirror.UnionDeriver
import zio.prelude.*

// Define instances for individual types
given Equal[Int] = Equal.make(_ == _)
given Equal[String] = Equal.make(_ == _)

// Automatic derivation for the union type
type MyUnion = Int | String
val eq = UnionDeriver.derive[Equal, Int | String]

eq.equal(1, 1) // true
eq.equal(1, "1") // false (different types)
```

## Hash

```scala
given Equal[Int] = Equal.make(_ == _)
given Hash[Int] = Hash.make(_.hashCode, _ == _)
given Equal[String] = Equal.make(_ == _)
given Hash[String] = Hash.make(_.hashCode, _ == _)

type MyUnion = Int | String
val hash = UnionDeriver.derive[Hash, Int | String]

hash.hash(42) // hash based on the type and value
```

## Dependencies

```scala
libraryDependencies += "dev.zio" %% "zio-prelude" % "version"
```

## TODO

- Implement `Ord` once the proper ZIO Prelude API is identified
