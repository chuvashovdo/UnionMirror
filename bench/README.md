# UnionMirror Performance Benchmarks

This project contains performance benchmarks for UnionMirror, using JMH (Java Microbenchmark Harness).

## Running benchmarks

### Run all benchmarks

```bash
sbt bench/jmh:run
```

### Run a specific benchmark

```bash
sbt 'bench/jmh:run -prof gc SamDerivationBench'
```

### Run with custom parameters

```bash
sbt 'bench/jmh:run -wi 5 -i 5 -f 2 -t 1'
```

Where:
- `-wi 5` - number of warmup iterations
- `-i 5` - number of measurement iterations
- `-f 2` - number of forks
- `-t 1` - number of threads

## Benchmarks

### SamDerivationBench

Measures SAM type-class derivation performance:
- Contravariant derivation (small and medium unions)
- Covariant derivation with try/catch fallback
- Covariant derivation with safe return types (Option, Either, Try)

### BinaryDerivationBench

Measures binary derivation performance via custom builders:
- `Eq` derivation for small and medium unions
- Runtime performance of binary operations

### LargeUnionBench

Measures scalability on large union types:
- Derivation for 5 and 10 types
- Runtime performance for large unions

## Results

Each run produces a console report containing:
- Compilation (derivation) time
- Runtime execution time
- Statistics (mean, standard deviation, etc.)

## Interpreting the results

- **Compilation time** - time spent generating code in the macro (relevant to build time)
- **Runtime time** - time spent executing operations (relevant to application performance)
- Comparison of try/catch vs. folding strategies for covariant derivation
