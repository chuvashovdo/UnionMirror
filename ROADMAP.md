# Roadmap

## Текущий статус проекта

**Версия:** 0.2.0 (WIP)

**Статус:** ✅ Основная функциональность реализована и протестирована

### Реализовано:

- ✅ Contravariant деривация (SAM + Builder)
- ✅ Covariant деривация (SAM + Builder)
- ✅ Binary деривация (Builder)
- ✅ Interop с cats (Show, Eq, Order, Hash)
- ✅ Interop с circe (Encoder, Decoder)
- ✅ Тесты реорганизованы в отдельные файлы по категориям (30+ сценариев)
- ✅ Документация границ применимости и результатов (CAPABILITIES.md)

**См. также:** [CAPABILITIES.md](CAPABILITIES.md) - подробное описание возможностей, ограничений и проблем библиотеки.

### В работе:

- 🔄 Исследование производительности

### Планы:

- 📋 Оптимизация fallback try-catch стратегии
- 📋 Поддержка Scala 3.4+
- 📋 Дополнительные interop модули (zio, fs2?)

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
    def build[T](elems: List[Decoder[Any]]): Decoder[T] =
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
    def build[T](ordinal: T => Int, elems: List[Eq[Any]]): Eq[T] =
      new Eq[T]:
        def eqv(x: T, y: T): Boolean =
          val ox = ordinal(x)
          val oy = ordinal(y)
          if ox != oy then false
          else elems(ox).eqv(x, y)

// Order
given UnionDeriver.BinaryInstanceBuilder[Order] =
  new UnionDeriver.BinaryInstanceBuilder[Order]:
    def build[T](ordinal: T => Int, elems: List[Order[Any]]): Order[T] =
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

## Нереализованные задачи

### ❌ Исследование производительности

**План:**

1. Сравнить время компиляции с/без деривации
2. Измерить рантайм-оверхед синтетических Mirror vs стандартные enum
3. Протестировать на больших union типах (10+ вариантов)

**Метрики для сбора:**

- Время компиляции (scalac)
- Размер байткода
- Время выполнения ordinal()
- Время выполнения деривированных операций (eqv, compare, show, encode)

## Структура проекта

```
core/
├── UnionDeriver.scala          # Public API для деривации
├── UnionMirror.scala          # Синтез Mirror.SumOf
└── internal/
    ├── UnionDeriverImpl.scala # Реализация деривации
    ├── UnionMirrorImpl.scala   # Реализация Mirror.SumOf
    ├── deriver/
    │   ├── ContravariantWithBuilderImpl.scala
    │   ├── ContravariantSamImpl.scala
    │   ├── CovariantWithBuilderImpl.scala
    │   ├── CovariantSamImpl.scala      # ✅ Реализован (fallback)
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
└── instances.scala            # Show, Eq, Order

interop-circe/
└── instances.scala            # Encoder, Decoder (builder)

tests/
├── SamTests.scala              # SAM деривация
├── BinaryInstanceTests.scala   # Бинарные операции
├── BuilderTests.scala          # Кастомные билдеры
├── AdvancedTypeTests.scala     # Продвинутые типы
├── UnionNormalizationTests.scala  # Нормализация Union
├── HierarchyTests.scala       # Иерархии типов
├── InteropTests.scala         # Интеграция с Cats/Circe
├── LargeUnionTests.scala      # Большие Union
├── MirrorInteropTests.scala   # Взаимодействие с Mirror
├── RecursiveTests.scala        # Рекурсивные структуры
└── CommonModels.scala         # Общие модели (Cat, Dog)
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
