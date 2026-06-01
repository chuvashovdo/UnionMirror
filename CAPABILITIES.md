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

- `Encoder` (contravariant) - автоматическая деривация
- `Decoder` (covariant) - автоматическая деривация через builder

```scala
import unionmirror.interop.circe.instances.given

val encoder = summon[io.circe.Encoder[Cat | Dog]]
val decoder = summon[io.circe.Decoder[Cat | Dog]]
```

**Примечание:** Для избежания конфликтов с макросами Circe для sealed traits с идентичными JSON-структурами можно использовать явную деривацию:

```scala
given m: Mirror.SumOf[T] = UnionMirror.synth[T]
val decoder = UnionDeriver.deriveCovariant[Decoder, T]
```

#### 6.3 ZIO Prelude

Поддерживаемые типклассы:

- `Equal` (binary)
- `Hash` (binary)

```scala
import unionmirror.interop.zio.instances.given
import zio.prelude.*

given Equal[Int] = Equal.make(_ == _)
given Equal[String] = Equal.make(_ == _)

val eq = UnionDeriver.derive[Equal, Int | String]
eq.equal(1, 1) // true
eq.equal(1, "1") // false (разные типы)
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

### 8. Автоматический синтез Mirror.SumOf

С помощью `import unionmirror.auto.given` библиотека предоставляет автоматический `given Mirror.SumOf[T]` для union types. Это позволяет использовать `summon[Mirror.SumOf[T]]` или получать его через implicit parameters без явного вызова `UnionMirror.synth[T]`.

```scala
import unionmirror.auto.given

type MyUnion = Int | String | Boolean

// Автоматически работает через summon
val mirror = summon[Mirror.SumOf[MyUnion]]

// Или через implicit parameters
def process[T](using m: Mirror.SumOf[T]): Unit = ???
process[Int | String] // Работает автоматически
```

### 9. Масштабируемость

Тесты подтверждают стабильную работу с Union из 15+ типов.

### 10. Адаптерная деривация

Возможно создавать собственные функции деривации на основе `UnionDeriver`:

```scala
inline def deriveEq[T](
  using m: scala.deriving.Mirror.SumOf[T],
  builder: UnionDeriver.BinaryInstanceBuilder[MyEq]
): MyEq[T] =
  UnionDeriver.deriveBinary[MyEq, T]
```

### 11. Оптимизированная fallback-стратегия для ковариантной деривации

Автоматическая деривация ковариантных SAM автоматически обнаруживает типы с безопасными возвращаемыми значениями (`Try`, `Either`, `Option`) и использует более эффективную стратегию folding вместо try-catch. Это обеспечивает нулевую стоимость для типклассов, которые уже используют функциональные подходы обработки ошибок.

Для типклассов с небезопасными возвращаемыми значениями используется try-catch fallback, который позволяет попробовать следующий парсер если текущий падает с исключением.

### 12. Бенчмарки производительности

Проект `bench` содержит JMH бенчмарки для систематического измерения производительности:

- Время деривации SAM типклассов (контравариантных и ковариантных)
- Сравнение try-catch vs folding стратегий для ковариантной деривации
- Время выполнения операций runtime
- Масштабируемость на больших union типах (5-10 типов)
- Производительность бинарной деривации через билдеры

Запуск:

```bash
sbt bench/jmh:run
```

**Результаты бенчмарков:**

- Деривация SAM типклассов: **1-3 наносекунд** (практически мгновенно)
- Деривация бинарных типклассов: **25-30 наносекунд**
- Деривация больших union (10 типов): **3 наносекунд** (без деградации)
- Runtime выполнение: **2-22 наносекунд**

Подробные результаты см. в [`bench/BENCHMARK_RESULTS.md`](../bench/BENCHMARK_RESULTS.md).

**Вывод:** Библиотека не создает заметной нагрузки на компилятор и имеет отличную runtime производительность.

## Ограничения и проблемы

### 1. Ограничения на типы

**Не поддерживаются:**

- Union типов с параметрами высшего порядка (например, `List[_] | Option[_]`)
- Union типов с зависимыми типами (path-dependent types)

**Частично поддерживаются:**

- **Refinement типы** (например, `{ def foo: Int } | String`): Библиотека автоматически извлекает базовый тип из refinement и использует его для деривации. Например, `Int { def foo: Int }` будет обработан как `Int`. Это позволяет использовать union с refinement типами, но информация о refinement теряется в runtime.

### 2. Отсутствие автоматической деривации для не-SAM типклассов

**Проблема:** Попытка реализации полноценной multi-method деривации столкнулась со сложностями в макросах Scala 3.

**Текущий статус:**

- Для SAM типклассов (один абстрактный метод) автоматическая деривация работает корректно
- Для типклассов с несколькими методами требуется явный билдер (`ContravariantInstanceBuilder`, `CovariantInstanceBuilder` или `BinaryInstanceBuilder`)

**Решение:** Реализация соответствующего билдера для сложного типкласса.

**Фундаментальное ограничение:** Это ограничение текущей архитектуры, которое требует дополнительного исследования и разработки для решения. Макросы Scala 3 не предоставляют удобных механизмов для автоматического анализа и комбинирования нескольких методов типкласса.

### 3. Отсутствие интеграционных модулей для FS2 и других

**✅ Частично решено:** Добавлен интеграционный модуль для ZIO Prelude с поддержкой `Equal` и `Hash`.

**Планируется:** Дополнительные interop модули (fs2 и другие библиотеки) в будущих версиях.

## Покрытие тестами

Тесты организованы в отдельные файлы по категориям:

- **SamTests.scala** - SAM деривация (контравариантная и ковариантная)
- **BinaryInstanceTests.scala** - Бинарные операции (Eq, Hash) и адаптерная деривация
- **BuilderTests.scala** - Кастомные билдеры
- **AdvancedTypeTests.scala** - Продвинутые типы (singleton, object, параметризованные, multi-param, Any, LSP)
- **UnionNormalizationTests.scala** - Нормализация и упорядочивание Union
- **HierarchyTests.scala** - Иерархии трейтов и классов
- **InteropTests.scala** - Интеграция с Cats и Circe
- **ZioInteropTests.scala** - Интеграция с ZIO Prelude
- **LargeUnionTests.scala** - Большие Union и производительность fallback
- **MirrorInteropTests.scala** - Взаимодействие со стандартным Mirror
- **RecursiveTests.scala** - Рекурсивные структуры

### Бенчмарки производительности

- **SamDerivationBench.scala** - Производительность SAM деривации
- **BinaryDerivationBench.scala** - Производительность бинарной деривации
- **LargeUnionBench.scala** - Масштабируемость на больших union

Всего: **30+ тестовых сценариев** и **3 набора бенчмарков**, покрывающих основные варианты использования и производительность библиотеки.

## Заключение

**UnionMirror** предоставляет мощный и гибкий механизм деривации типклассов для Union типов в Scala 3. Библиотека особенно полезна для:

- Создания API с Union типами вместо иерархий
- Интеграции с популярными библиотеками (Cats, Circe, ZIO Prelude)
- Работа с сложными типами (параметризованные, рекурсивные, иерархии)
- Сценариев, где требуется динамическая диспетчеризация по типу
- Измерения производительности через JMH бенчмарки

**Основные ограничения:**

- Автоматическая деривация работает только для SAM типклассов (один абстрактный метод)
- Для типклассов с несколькими методами требуется явный билдер
- Некоторые ограничения на типы (параметры высшего порядка, зависимые типы, refinement типы)

Для большинства практических сценариев библиотека полностью функциональна и стабильна.
