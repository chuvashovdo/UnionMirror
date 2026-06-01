# Roadmap

## Текущий статус проекта

**Версия:** 0.2.0

**Статус:** ✅ Основная функциональность реализована и протестирована

### Реализовано:

- ✅ Contravariant деривация (SAM + Builder)
- ✅ Covariant деривация (SAM + Builder, с safe-type fallback для `Try`/`Either`/`Option`)
- ✅ Binary деривация (Builder)
- ✅ Interop с cats (Show, Eq, Order, Hash)
- ✅ Interop с circe (Encoder, Decoder с агрегацией ошибок)
- ✅ Interop с ZIO Prelude (Equal, Hash)
- ✅ Автоматический синтез `Mirror.SumOf` через `unionmirror.auto.given`
- ✅ Универсальный `UnionDeriver.derive` с диспетчеризацией по типу билдера
- ✅ Корректная дедупликация параметризованных типов (`List[Int] | List[String]`)
- ✅ JMH бенчмарки (см. `bench/BENCHMARK_RESULTS.md`)
- ✅ Тесты реорганизованы в отдельные файлы по категориям (40+ сценариев)
- ✅ Документация границ применимости и результатов (CAPABILITIES.md)

**См. также:** [CAPABILITIES.md](CAPABILITIES.md) — подробное описание возможностей, ограничений и проблем библиотеки.

### Планы:

- 📋 Поддержка Scala 3.4+ LTS (текущая версия: 3.8.1)
- 📋 Дополнительные interop модули (fs2, scodec)
- 📋 Публикация артефактов (Maven Central / Sonatype)
- 📋 Исследование multi-method деривации без билдера

## Реализованные задачи

### ✅ deriveCovariant (Производящие тайпклассы)

**Статус:** Полностью реализован (builder + SAM)

**Описание:**

- Добавлен `CovariantInstanceBuilder[F[_]]` в `UnionDeriver`
- Реализован `CovariantWithBuilderImpl` для типклассов, которые принимают список инстансов
- Реализован `CovariantSamImpl` для автоматической деривации ковариантных SAM-типклассов
- Fallback-стратегия: последовательный перебор инстансов с try-catch до первого успеха

**Пример использования (circe - builder):**

```scala
given UnionDeriver.CovariantInstanceBuilder[Decoder] =
  new UnionDeriver.CovariantInstanceBuilder[Decoder]:
    def build[T](elems: IndexedSeq[Decoder[Any]]): Decoder[T] =
      Decoder.instance { (c: io.circe.HCursor) =>
        elems.foldLeft[Decoder.Result[T]](...) { (acc, decoder) =>
          // fallback: пробуем каждый decoder по очереди
        }
      }

inline given [T](using Mirror.SumOf[T]): Decoder[T] =
  UnionDeriver.deriveCovariant[Decoder, T]
```

**Пример использования (SAM - автоматическая деривация):**

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Cat] = (s: String) => /* парсинг Cat */
given Parser[Dog] = (s: String) => /* парсинг Dog */

val p = UnionDeriver.deriveCovariant[Parser, Cat | Dog]
// Автоматически генерирует класс с fallback-логикой
```

**Ограничения:**

- Для сложных типклассов (не-SAM) требуется явный `CovariantInstanceBuilder`
- Fallback использует try-catch, что может быть медленно для высоконагруженных систем

### ✅ deriveBinary (Бинарные операции)

**Статус:** Реализован

**Описание:**

- Добавлен `BinaryInstanceBuilder[F[_]]` в `UnionDeriver`
- Реализован `BinaryWithBuilderImpl` для типклассов с бинарными операциями
- Логика: сравнение ординалов сначала, затем делегирование инстансу конкретного типа

**Пример использования (cats):**

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

**Тесты:**

- `Eq[Cat | Dog]` - значения разных типов не равны, одного типа - делегирование
- `Order[Cat | Dog]` - разные типы сравниваются по ordinal, одинаковые - делегирование

## Завершённые исследования

### ✅ Производительность

См. `bench/BENCHMARK_RESULTS.md`. JMH-бенчмарки покрывают:

- Деривацию SAM-типклассов (контравариантные и ковариантные)
- Сравнение try-catch vs folding для covariant SAM
- Бинарную деривацию через билдеры
- Масштабируемость на больших union (5–10 типов)

C версии 0.2.0 массив инстансов аллоцируется один раз на деривацию (раньше — на каждый вызов SAM-метода).

## Структура проекта

```
core/
├── UnionDeriver.scala               # Public API для деривации
├── UnionMirror.scala                # Синтез Mirror.SumOf
├── auto/package.scala               # Автоматический given Mirror.SumOf
└── internal/
    ├── UnionDeriverImpl.scala       # Splice-обёртки для макро-импл
    ├── UnionMirrorImpl.scala        # Реализация Mirror.SumOf
    ├── TupleTypeBuilder.scala       # Сборка Tuple типов из List[TypeRepr]
    ├── deriver/
    │   ├── AutoSamImpl.scala        # derive[F,T] auto SAM dispatch
    │   ├── ContravariantWithBuilderImpl.scala
    │   ├── ContravariantSamImpl.scala
    │   ├── CovariantWithBuilderImpl.scala
    │   ├── CovariantSamImpl.scala   # SAM с safe-type fallback
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
└── instances.scala                  # Encoder, Decoder (builder с агрегацией ошибок)

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

## Примеры использования

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
eq.eqv(Cat("m"), Dog("m")) // false (разные типы)
```

### Covariant (Decoder) - через builder

```scala
given Decoder[Cat] = ...
given Decoder[Dog] = ...

val dec = summon[Decoder[Cat | Dog]]
// Пробует Decoder[Cat], затем Decoder[Dog]
```

### Covariant (Parser) - SAM автоматическая деривация

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Cat] = (s: String) => Cat(s)
given Parser[Dog] = (s: String) => Dog(s)

val p = UnionDeriver.deriveCovariant[Parser, Cat | Dog]
// Автоматически генерирует класс с fallback-логикой
p.parse("cat:m") // Cat("m")
p.parse("dog:b") // Dog("b")
```

## Покрытие тестами

Тесты реорганизованы в отдельные файлы по категориям (30+ сценариев):

### SamTests.scala (3 теста)

- Контравариантный SAM - `Printer[Int | String]`
- Ковариантный SAM с fallback - `Parser[Int | Boolean]`
- Ковариантный SAM с кастомными классами - `Parser[Cat | Dog]`

### BinaryInstanceTests.scala (3 теста)

- Бинарный типкласс через Builder - `Eq[Int | String]`
- Cats Hash interop - `Hash[Int | String | Boolean]`
- Адаптерная деривация - явное использование `Mirror.SumOf`

### BuilderTests.scala (2 теста)

- Кастомный ContravariantInstanceBuilder - `Logger[Int | String | Boolean]`
- Кастомный CovariantInstanceBuilder - `Factory[Int | String | Boolean]`

### AdvancedTypeTests.scala (8 тестов)

- Singleton типы - `1 | "a" | true`
- Object-типы - `MyObject1.type | MyObject2.type`
- Параметризованные трейты - `Container[Int] | Container[String]`
- Параметризованные типы - `List[Int] | Option[String] | Vector[Int]`
- Типы с несколькими параметрами - `Either[String, Int] | (Int, String) | Map[String, Int]`
- Union с Any - `Int | String | Any`
- LSP с общими методами - `Drawable | (Circle | Square)`

### UnionNormalizationTests.scala (5 тестов)

- Вложенные Union и нормализация - `Int | (String | Boolean)`
- Нормализация и порядок элементов
- Стабильность ординалов между вызовами
- Проверка ошибок для не-Union типов
- Проверка ошибок для не-SAM типов без builder

### HierarchyTests.scala (4 теста)

- Иерархия типов - `Shape | Circle`
- Sealed trait union - `Shape with Circle | Rectangle`
- Сложная иерархия трейтов - `Animal | (Mammal | (Dog | Cat))`
- Вложенные Union с трейтами - `Animal | (Plant | (Tree | Flower))`

### InteropTests.scala (6 тестов)

- Cats Show interop - `Show[Cat] + Show[Dog] => Show[Cat | Dog]`
- Cats Eq interop - `Eq[Cat] + Eq[Dog] => Eq[Cat | Dog]`
- Cats Order interop - `Order[Cat] + Order[Dog] => Order[Cat | Dog]`
- Cats deep interop - `Order[Int | String | Boolean]`
- Circe Decoder interop - `Decoder[LocalCat | LocalDog | Int]`
- Circe Encoder interop - `Encoder[LocalCat | LocalDog | Int]`

### LargeUnionTests.scala (2 теста)

- Большой Union - 15 типов
- Производительность fallback с несколькими типами

### MirrorInteropTests.scala (3 теста)

- Взаимодействие с системным Mirror (enum) - `MyEnum | (Cat | Dog)`
- Scala 3 enum Mirror interop - `StandardEnum | Int`
- Scala 3 sealed trait Mirror interop - `StandardSealed | String`

### RecursiveTests.scala (2 теста)

- Рекурсивный Union - `Tree = Leaf | Node`
- Адаптерная деривация с рекурсивными типами - `Serialize[RecursiveTree]`
