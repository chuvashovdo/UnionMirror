# UnionMirror ZIO Prelude Interop

Интеграционный модуль для [ZIO Prelude](https://zio.dev/zio-prelude/), предоставляющий автоматическую деривацию типклассов для union типов.

## Поддерживаемые типклассы

- `Equal` - бинарная деривация
- `Hash` - бинарная деривация

## Использование

```scala
import unionmirror.interop.zio.instances.given
import unionmirror.UnionDeriver
import zio.prelude.*

// Определение инстансов для отдельных типов
given Equal[Int] = Equal.make(_ == _)
given Equal[String] = Equal.make(_ == _)

// Автоматическая деривация для union типа
type MyUnion = Int | String
val eq = UnionDeriver.derive[Equal, Int | String]

eq.equal(1, 1) // true
eq.equal(1, "1") // false (разные типы)
```

## Hash

```scala
given Equal[Int] = Equal.make(_ == _)
given Hash[Int] = Hash.make(_.hashCode, _ == _)
given Equal[String] = Equal.make(_ == _)
given Hash[String] = Hash.make(_.hashCode, _ == _)

type MyUnion = Int | String
val hash = UnionDeriver.derive[Hash, Int | String]

hash.hash(42) // хеш на основе типа и значения
```

## Зависимости

```scala
libraryDependencies += "dev.zio" %% "zio-prelude" % "version"
```

## TODO

- Реализация `Ord` после изучения правильного API ZIO Prelude
