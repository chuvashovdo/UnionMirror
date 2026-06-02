# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
with the `early-semver` scheme.

## [1.0.0]

### Added

- `MIT` `LICENSE` file at the repository root.
- `interop-zio`: automatic `inline given Equal[T]` and `inline given Hash[T]`
  for symmetry with `interop-cats` / `interop-circe` (no need to call
  `UnionDeriver.derive` explicitly anymore).

### Changed

- **BREAKING**: unions containing a top type (`Any`, `AnyRef`/`Object`,
  `Matchable`) are now rejected at compile time. Such a union is equivalent
  to the top type itself (`Int | String | Any =:= Any`), so a
  `Mirror.SumOf` could not soundly distinguish its members; the previous
  "`Any` as fallback" behaviour relied on the compiler not collapsing the
  hand-written union and was unsound. Derive an instance for the top type
  directly if you need total coverage.
- **BREAKING**: `UnionDeriver.CovariantInstanceBuilder.build` and
  `UnionDeriver.BinaryInstanceBuilder.build` now take `IndexedSeq[F[Any]]`
  instead of `List[F[Any]]`. This gives `O(1)` element access in builder
  implementations (the previous `List(ox)` was `O(n)`). All bundled
  interop builders updated; user-defined builders need a one-line
  signature change.
- **Performance, for real this time**: the `instances` array in the
  synthesized SAM class is now stored as a private field of the generated
  class instead of being reallocated inside the SAM method body on every
  invocation. The 0.2 changelog claimed this was already done — it
  wasn't; this release actually does it.
- `interop-zio.Hash` now mixes the union ordinal into the hash
  (`31 * ox + h`) like `interop-cats.Hash`, instead of just returning the
  underlying instance hash. Improves hash dispersion for unions where
  members may share underlying hash values.
- `package object auto` replaced with top-level definitions in
  `unionmirror.auto` (Scala 3 idiom; `package object` is deprecated).
- All macro-implementation objects under `unionmirror.internal.*` are now
  `private[unionmirror]` (they were technically public before).
- circe `Decoder` builder now iterates by index instead of pattern-matching
  a `List` (consequence of the `IndexedSeq` switch).

## [0.2.0]

### Added

- `interop-zio` module with `Equal` and `Hash` builders for ZIO Prelude.
- `interop-cats`: `BinaryInstanceBuilder[Hash]` and an automatic `given Hash[T]`.
- `unionmirror.auto.given mirrorSumOf[T]` for implicit `Mirror.SumOf` resolution.
- `UnionDeriver.derive[F, T]` universal entry point that picks the right
  builder/SAM strategy.
- Safe-result fallback in covariant SAM deriver: for return types
  `Try`/`Either`/`Option` the generated dispatcher folds over results instead of
  using `try`/`catch`.
- Aggregated error message in the circe `Decoder` builder — all attempted
  decoders' failures are now surfaced.
- Regression tests for parametrized-type deduplication and singleton-literal
  unions.

### Changed

- `UnionKeys.stableKey` is now structural — distinguishes parametrized types
  (`List[Int]` vs `List[String]`) and singleton literals (`1` vs `2`) which
  were previously collapsed by `distinctStable`.
- `UnionMirror.synth` now emits a compile-time warning when two or more
  union elements share the same JVM erasure (e.g. `List[Int] | List[String]`),
  since `mirror.ordinal` cannot discriminate them at runtime.
- Covariant SAM `Match`/safe-type fallback no longer evaluates the underlying
  call twice on success — the result is bound to a fresh `val` first.
- Error message in `DeriverSamAnalysis` is no longer specific to the
  contravariant builder.
- `UnionSort.pickMinByKey` replaced with the standard `List.minBy`.
- `CovariantSamImpl` and `CovariantWithBuilderImpl` no longer accept the unused
  `Mirror.SumOf` macro argument.

### Fixed

- `List[Int] | List[String]` and similar parametrized unions are no longer
  collapsed into a single element by the normalizer.
- Singleton-literal unions like `1 | 2` now produce distinct ordinals.
- Removed dead `if !(TypeRepr.of[T] <:< argTpe) then ()` in
  `DeriverSamAnalysis`.

## [0.1.0]

Initial implementation:

- `UnionMirror.synth[T]` / `UnionMirror.derived[T]` for synthesizing
  `Mirror.SumOf` of union types.
- Contravariant / Covariant / Binary derivation strategies with custom builder
  support.
- Union normalization (flatten, dedup, topological sort by subtype + name).
- Cats interop (Show, Eq, Order).
- Circe interop (Encoder, Decoder).
- JMH benchmarks.
