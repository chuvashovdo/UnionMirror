# UnionMirror: Границы применимости и результаты

## Обзор

**UnionMirror** - это библиотека для Scala 3, которая предоставляет механизмы деривации типклассов для Union типов (`A | B | C`) через синтез `Mirror.SumOf`. Библиотека позволяет автоматически создавать инстансы типклассов для Union типов на основе инстансов для каждого составляющего типа.

## Что можно делать с помощью библиотеки

### 1. Деривация типклассов для Union типов

Библиотека поддерживает три основных стратегии деривации:

#### 1.1 Контравариантная деривация (Contravariant)

Подходит для типклассов вида `F[-T]` (например, `Show`, `Encoder`, `Logger`).

**Работает автоматически для SAM-типклассов:**

```scala
trait Printer[-T]:
  def print(value: T): String

given Printer[Int] = (value: Int) => s"int:$value"
given Printer[String] = (value: String) => s"str:$value"

val p = UnionDeriver.deriveContravariant[Printer, Int | String]
p.print(42) // "int:42"
p.print("hello") // "str:hello"
```

**Поддержка кастомных билдеров:**

```scala
given UnionDeriver.ContravariantInstanceBuilder[Logger] =
  new UnionDeriver.ContravariantInstanceBuilder[Logger]:
    def build[T](dispatch: T => Logger[Any]): Logger[T] = ...
```

#### 1.2 Ковариантная деривация (Covariant)

Подходит для типклассов вида `F[+T]` (например, `Parser`, `Decoder`, `Factory`).

**Автоматическая деривация для SAM с fallback-стратегией:**

```scala
trait Parser[+T]:
  def parse(s: String): T

given Parser[Int] = (s: String) => s.toInt
given Parser[Boolean] = (s: String) => s.toBoolean

val p = UnionDeriver.deriveCovariant[Parser, Int | Boolean]
p.parse("123") // 123
p.parse("true") // true
// Fallback: если один парсер падает, пробует следующий
```

**Поддержка кастомных билдеров для сложных типклассов:**

```scala
given UnionDeriver.CovariantInstanceBuilder[Decoder] =
  new UnionDeriver.CovariantInstanceBuilder[Decoder]:
    def build[T](elems: List[Decoder[Any]]): Decoder[T] = ...
```

#### 1.3 Бинарная деривация (Binary)

Подходит для типклассов с бинарными операциями (например, `Eq`, `Order`, `Hash`).

```scala
given UnionDeriver.BinaryInstanceBuilder[Eq] =
  new UnionDeriver.BinaryInstanceBuilder[Eq]:
    def build[T](ordinal: T => Int, elems: List[Eq[Any]]): Eq[T] =
      Eq.instance { (x, y) =>
        val ox = ordinal(x)
        val oy = ordinal(y)
        ox == oy && elems(ox).eqv(x, y)
      }

val eq = UnionDeriver.deriveBinary[Eq, Int | String]
eq.eqv(1, 1) // true
eq.eqv(1, "1") // false (разные типы)
```

### 2. Работа со сложными типами

#### 2.1 Singleton типы и литералы

```scala
type LiteralUnion = 1 | "a" | true
```

#### 2.2 Object-типы

```scala
type ObjectUnion = MyObject1.type | MyObject2.type
```

#### 2.3 Параметризованные типы

```scala
type ParamUnion = List[Int] | Option[String] | Vector[Int]
```

#### 2.4 Типы с несколькими параметрами

```scala
type MultiParamUnion = Either[String, Int] | (Int, String) | Map[String, Int]
```

#### 2.5 Union с `Any`

```scala
type AnyUnion = Int | String | Any
// Инстанс для Any используется как fallback
```

#### 2.6 Параметризованные трейты

```scala
trait Container[+A]:
  def value: A

type ContUnion = IntContainer | StringContainer | BooleanContainer
```

### 3. Иерархии типов и LSP

Библиотека корректно обрабатывает иерархии типов, выбирая наиболее специфичный инстанс:

```scala
trait Shape:
  def area: Double
case class Circle(radius: Double) extends Shape

given Show[Circle] = (c: Circle) => s"Circle(r=${c.radius})"
given Show[Shape] = (s: Shape) => s"Shape(area=${s.area})"

// Для Circle будет выбран Show[Circle], а не Show[Shape]
```

Поддерживаются:

- Многоуровневые иерархии трейтов
- Sealed trait и его наследники
- Типы с общими методами (LSP проверка)
- Вложенные Union с трейтами

### 4. Рекурсивные структуры

```scala
case class Leaf(value: Int)
case class Node(left: Tree, right: Tree)
type Tree = Leaf | Node

given Show[Tree] = UnionDeriver.deriveContravariant[Show, Tree]
```

### 5. Нормализация и упорядочивание Union

Библиотека автоматически:

- Разворачивает вложенные Union: `Int | (String | Boolean)` → `Int | String | Boolean`
- Удаляет дубликаты: `Int | Int | String` → `Int | String`
- Сортирует типы для стабильности ординалов
- Обеспечивает стабильность ординалов между вызовами

### 6. Интеграция с экосистемой Scala

#### 6.1 Cats

Поддерживаемые типклассы:

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

Поддерживаемые типклассы:

- `Encoder` (contravariant)
- `Decoder` (covariant через builder)

```scala
import unionmirror.interop.circe.instances.given

val encoder = summon[io.circe.Encoder[Cat | Dog]]
val decoder = summon[io.circe.Decoder[Cat | Dog]]
```

### 7. Взаимодействие со стандартным Mirror

Библиотека может работать в паре со стандартным `Mirror` Scala 3:

```scala
enum MyEnum:
  case A, B

type MixedUnion = MyEnum | Cat | Dog
given m: scala.deriving.Mirror.SumOf[MixedUnion] = UnionMirror.synth[MixedUnion]
```

Поддерживаются:

- Scala 3 enum
- Sealed trait
- Смешивание системных и синтетических Mirror

### 8. Масштабируемость

Тесты подтверждают стабильную работу с Union из 15+ типов.

### 9. Адаптерная деривация

Возможно создавать собственные функции деривации на основе `UnionDeriver`:

```scala
inline def deriveEq[T](
  using m: scala.deriving.Mirror.SumOf[T],
  builder: UnionDeriver.BinaryInstanceBuilder[MyEq]
): MyEq[T] =
  UnionDeriver.deriveBinary[MyEq, T]
```

## Ограничения и проблемы

### 1. Fallback-стратегия в ковариантной деривации

**Проблема:** Автоматическая деривация ковариантных SAM использует try-catch fallback, что может быть медленно для высоконагруженных систем.

**Решение:** Для производительности рекомендуется использовать явный `CovariantInstanceBuilder` с оптимизированной логикой.

### 2. Необходимость явного синтеза Mirror

**Проблема:** Для Union типов требуется явный вызов `UnionMirror.synth[T]` для создания `Mirror.SumOf[T]`.

**Решение:** Использование `import unionmirror.auto.given` для автоматического синтеза (где поддерживается).

### 3. Ограничения на типы

**Не поддерживаются:**

- Union типов с параметрами высшего порядка (например, `List[_] | Option[_]`)
- Union типов с зависимыми типами (path-dependent types)
- Union типов с refinement типами (например, `{ def foo: Int } | String`)

### 4. Несовместимость с некоторыми макросами

**Проблема:** Конфликты с макросами Circe при использовании `summon` для декодеров с идентичными JSON-структурами.

**Решение:** Использование явной деривации через `UnionDeriver.deriveCovariant` вместо `summon`.

### 5. Отсутствие автоматической деривации для не-SAM типклассов

**Проблема:** Для сложных типклассов с несколькими методами требуется явный билдер.

**Решение:** Реализация `ContravariantInstanceBuilder`, `CovariantInstanceBuilder` или `BinaryInstanceBuilder`.

### 6. Синтаксические требования Scala 3

**Проблема:** Требуется строгий синтаксис `given ... :` вместо устаревшего `given ... with`.

**Решение:** Использование актуального синтаксиса Scala 3.

### 7. Inline-требования для адаптерных методов

**Проблема:** Методы-адаптеры должны быть помечены как `inline` для корректной работы макросов.

**Решение:** Добавление модификатора `inline` к адаптерным функциям.

### 8. Отсутствие интеграционных модулей для ZIO, FS2 и других

**Проблема:** Нет готовых интеграций с популярными библиотеками экосистемы Scala.

**Планируется:** Дополнительные interop модули (zio, fs2?) в будущих версиях.

### 9. Отсутствие бенчмарков производительности

**Проблема:** Нет систематических измерений производительности (время компиляции, рантайм, размер байткода).

**Планируется:** Исследование производительности и оптимизация fallback try-catch стратегии.

## Покрытие тестами

Тесты организованы в отдельные файлы по категориям:

- **SamTests.scala** - SAM деривация (контравариантная и ковариантная)
- **BinaryInstanceTests.scala** - Бинарные операции (Eq, Hash) и адаптерная деривация
- **BuilderTests.scala** - Кастомные билдеры
- **AdvancedTypeTests.scala** - Продвинутые типы (singleton, object, параметризованные, multi-param, Any, LSP)
- **UnionNormalizationTests.scala** - Нормализация и упорядочивание Union
- **HierarchyTests.scala** - Иерархии трейтов и классов
- **InteropTests.scala** - Интеграция с Cats и Circe
- **LargeUnionTests.scala** - Большие Union и производительность fallback
- **MirrorInteropTests.scala** - Взаимодействие со стандартным Mirror
- **RecursiveTests.scala** - Рекурсивные структуры

Всего: **30+ тестовых сценариев**, покрывающих основные варианты использования библиотеки.

## Заключение

**UnionMirror** предоставляет мощный и гибкий механизм деривации типклассов для Union типов в Scala 3. Библиотека особенно полезна для:

- Создания API с Union типами вместо иерархий
- Интеграции с популярными библиотеками (Cats, Circe)
- Работа с сложными типами (параметризованные, рекурсивные, иерархии)
- Сценариев, где требуется динамическая диспетчеризация по типу

Основные ограничения связаны с производительностью fallback-стратегии и необходимостью явной конфигурации для сложных типклассов. Для большинства практических сценариев библиотека полностью функциональна и стабильна.
