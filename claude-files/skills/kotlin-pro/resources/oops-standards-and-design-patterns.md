# OOP Standards & Design Patterns (Java/Kotlin)

## Core OOP Principles

### The Four Pillars

1. **Encapsulation** — Bundle data and methods together; hide internal state, expose behavior through public APIs.
2. **Abstraction** — Expose only relevant details; hide complexity behind interfaces and abstract classes.
3. **Inheritance** — Derive new classes from existing ones to reuse and extend behavior.
4. **Polymorphism** — One interface, multiple implementations. Achieved via method overriding (runtime) and overloading (compile-time).

---

## SOLID Principles

| Principle | Description | Example |
|---|---|---|
| **S** — Single Responsibility | A class should have only one reason to change | `UserValidator` validates, `UserRepository` persists — don't merge them |
| **O** — Open/Closed | Open for extension, closed for modification | Add new behavior via new classes/interfaces, not by editing existing ones |
| **L** — Liskov Substitution | Subtypes must be substitutable for their base types | If `Bird` has `fly()`, `Penguin extends Bird` violates LSP |
| **I** — Interface Segregation | Prefer many small interfaces over one fat interface | Split `Worker { work(), eat() }` into `Workable` and `Feedable` |
| **D** — Dependency Inversion | Depend on abstractions, not concretions | Constructor takes `PaymentGateway` interface, not `StripeGateway` class |

---

## Other Key OOP Principles

- **Composition over Inheritance** — Favor `has-a` over `is-a`. Compose behavior from smaller objects rather than deep class hierarchies.
- **Program to an interface, not an implementation** — Declare variables and parameters as interface types.
- **DRY** (Don't Repeat Yourself) — Extract common logic; but don't over-abstract for just two occurrences.
- **YAGNI** (You Aren't Gonna Need It) — Don't build for hypothetical future requirements.
- **Law of Demeter** — A method should only call methods on: itself, its parameters, objects it creates, its direct fields. Avoid `a.getB().getC().doThing()`.
- **Tell, Don't Ask** — Tell objects what to do instead of querying their state and deciding externally.

---

## GoF Design Patterns (Gang of Four)

### Creational Patterns — How objects are created

#### 1. Singleton
**Intent:** Ensure a class has exactly one instance with a global access point.

```kotlin
// Kotlin — built-in thread-safe singleton
object DatabaseConnection {
    fun query(sql: String): Result { /* ... */ }
}
```

```java
// Java — thread-safe with enum
public enum DatabaseConnection {
    INSTANCE;
    public Result query(String sql) { /* ... */ }
}
```

**When to use:** Logging, configuration, connection pools, caches.
**Watch out:** Overuse makes testing hard. Prefer dependency injection.

---

#### 2. Factory Method
**Intent:** Define an interface for creating objects, but let subclasses decide which class to instantiate.

```kotlin
interface Notification {
    fun send(message: String)
}

class EmailNotification : Notification {
    override fun send(message: String) { /* send email */ }
}

class SmsNotification : Notification {
    override fun send(message: String) { /* send SMS */ }
}

// Factory method
fun createNotification(type: String): Notification = when (type) {
    "email" -> EmailNotification()
    "sms" -> SmsNotification()
    else -> throw IllegalArgumentException("Unknown type: $type")
}
```

**When to use:** When the exact type to create depends on runtime input or configuration.

---

#### 3. Abstract Factory
**Intent:** Create families of related objects without specifying their concrete classes.

```kotlin
interface UIFactory {
    fun createButton(): Button
    fun createTextField(): TextField
}

class MaterialUIFactory : UIFactory {
    override fun createButton() = MaterialButton()
    override fun createTextField() = MaterialTextField()
}

class CupertinoUIFactory : UIFactory {
    override fun createButton() = CupertinoButton()
    override fun createTextField() = CupertinoTextField()
}
```

**When to use:** Cross-platform UIs, database drivers, theme systems.

---

#### 4. Builder
**Intent:** Construct complex objects step-by-step, separating construction from representation.

```kotlin
// Kotlin — often replaced by named arguments + defaults
data class HttpRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

val request = HttpRequest(
    url = "https://api.example.com/users",
    method = "POST",
    body = """{"name": "Alice"}"""
)
```

```java
// Java — classic Builder pattern
public class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;

    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
    }

    public static class Builder {
        private final String url;
        private String method = "GET";
        private Map<String, String> headers = new HashMap<>();

        public Builder(String url) { this.url = url; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder header(String key, String value) { headers.put(key, value); return this; }
        public HttpRequest build() { return new HttpRequest(this); }
    }
}
```

**When to use:** Objects with many optional parameters, immutable objects with complex construction.

---

#### 5. Prototype
**Intent:** Create new objects by cloning existing ones.

```kotlin
// Kotlin — data class copy()
data class ServerConfig(
    val host: String,
    val port: Int,
    val ssl: Boolean = false
)

val production = ServerConfig("prod.example.com", 443, ssl = true)
val staging = production.copy(host = "staging.example.com")
```

**When to use:** When object creation is expensive, or you need variations of a base configuration.

---

### Structural Patterns — How objects are composed

#### 6. Adapter
**Intent:** Make incompatible interfaces work together.

```kotlin
// Existing interface from a third-party library
interface LegacyPrinter {
    fun printDocument(text: String)
}

// Our system's interface
interface ModernPrinter {
    fun print(content: Content)
}

// Adapter bridges the gap
class PrinterAdapter(private val legacy: LegacyPrinter) : ModernPrinter {
    override fun print(content: Content) {
        legacy.printDocument(content.toPlainText())
    }
}
```

**When to use:** Integrating third-party libraries, legacy system migration.

---

#### 7. Decorator
**Intent:** Add behavior dynamically without modifying the original class.

```kotlin
// Kotlin — using `by` delegation
interface DataSource {
    fun read(): String
    fun write(data: String)
}

class FileDataSource(private val path: String) : DataSource {
    override fun read(): String = File(path).readText()
    override fun write(data: String) { File(path).writeText(data) }
}

// Decorator adds encryption transparently
class EncryptedDataSource(
    private val source: DataSource
) : DataSource by source {
    override fun read(): String = decrypt(source.read())
    override fun write(data: String) = source.write(encrypt(data))
}

// Stack decorators
val source = EncryptedDataSource(FileDataSource("data.txt"))
```

**When to use:** Adding logging, caching, compression, encryption as layers. Java I/O streams use this extensively.

---

#### 8. Facade
**Intent:** Provide a simplified interface to a complex subsystem.

```kotlin
class OrderFacade(
    private val inventory: InventoryService,
    private val payment: PaymentService,
    private val shipping: ShippingService
) {
    fun placeOrder(order: Order): OrderResult {
        inventory.reserve(order.items)
        val receipt = payment.charge(order.total)
        val tracking = shipping.ship(order.address, order.items)
        return OrderResult(receipt, tracking)
    }
}
```

**When to use:** Simplifying complex library APIs, providing a clean entry point to a subsystem.

---

#### 9. Proxy
**Intent:** Provide a surrogate or placeholder to control access to another object.

```kotlin
interface Image {
    fun display()
}

class RealImage(private val path: String) : Image {
    init { loadFromDisk() } // expensive
    private fun loadFromDisk() { /* heavy I/O */ }
    override fun display() { /* render */ }
}

// Lazy-loading proxy
class LazyImage(private val path: String) : Image {
    private val realImage by lazy { RealImage(path) }
    override fun display() = realImage.display()
}
```

**When to use:** Lazy initialization, access control, logging, caching.

---

#### 10. Composite
**Intent:** Treat individual objects and compositions uniformly using tree structures.

```kotlin
interface FileSystemNode {
    fun size(): Long
    fun name(): String
}

class File(private val name: String, private val bytes: Long) : FileSystemNode {
    override fun size() = bytes
    override fun name() = name
}

class Directory(
    private val name: String,
    private val children: MutableList<FileSystemNode> = mutableListOf()
) : FileSystemNode {
    fun add(node: FileSystemNode) { children.add(node) }
    override fun size() = children.sumOf { it.size() }
    override fun name() = name
}
```

**When to use:** Tree structures — file systems, UI component trees, org charts.

---

#### 11. Bridge
**Intent:** Separate abstraction from implementation so both can vary independently.

```kotlin
// Implementation hierarchy
interface Renderer {
    fun renderCircle(radius: Double)
    fun renderSquare(side: Double)
}

class VectorRenderer : Renderer { /* SVG rendering */ }
class RasterRenderer : Renderer { /* Pixel rendering */ }

// Abstraction hierarchy
abstract class Shape(protected val renderer: Renderer) {
    abstract fun draw()
}

class Circle(renderer: Renderer, val radius: Double) : Shape(renderer) {
    override fun draw() = renderer.renderCircle(radius)
}
```

**When to use:** When both the abstraction and implementation need independent extension (e.g., shapes x renderers, platforms x features).

---

#### 12. Flyweight
**Intent:** Share common state across many fine-grained objects to reduce memory usage.

```kotlin
class CharacterStyle private constructor(
    val font: String,
    val size: Int,
    val color: String
) {
    companion object {
        private val cache = mutableMapOf<String, CharacterStyle>()

        fun of(font: String, size: Int, color: String): CharacterStyle {
            val key = "$font-$size-$color"
            return cache.getOrPut(key) { CharacterStyle(font, size, color) }
        }
    }
}
```

**When to use:** Large numbers of similar objects — text editors, game particles, map tiles.

---

### Behavioral Patterns — How objects communicate

#### 13. Strategy
**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

```kotlin
// Kotlin — lambdas often replace the full pattern
fun processPayment(
    amount: Double,
    strategy: (Double) -> PaymentResult
): PaymentResult = strategy(amount)

// Usage
val result = processPayment(100.0) { amount ->
    stripe.charge(amount) // or paypal.charge(amount)
}
```

```kotlin
// Classic approach with sealed interface
sealed interface CompressionStrategy {
    fun compress(data: ByteArray): ByteArray
}

object GzipCompression : CompressionStrategy {
    override fun compress(data: ByteArray) = /* gzip */ data
}

object ZipCompression : CompressionStrategy {
    override fun compress(data: ByteArray) = /* zip */ data
}
```

**When to use:** Multiple algorithms for the same task, chosen at runtime.

---

#### 14. Observer
**Intent:** Define a one-to-many dependency so that when one object changes state, all dependents are notified.

```kotlin
class EventBus {
    private val listeners = mutableMapOf<String, MutableList<(Any) -> Unit>>()

    fun subscribe(event: String, listener: (Any) -> Unit) {
        listeners.getOrPut(event) { mutableListOf() }.add(listener)
    }

    fun publish(event: String, data: Any) {
        listeners[event]?.forEach { it(data) }
    }
}
```

**When to use:** UI event handling, pub/sub systems, reactive data flows.

---

#### 15. Command
**Intent:** Encapsulate a request as an object, enabling undo/redo, queuing, and logging.

```kotlin
interface Command {
    fun execute()
    fun undo()
}

class InsertTextCommand(
    private val document: Document,
    private val position: Int,
    private val text: String
) : Command {
    override fun execute() { document.insert(position, text) }
    override fun undo() { document.delete(position, text.length) }
}

class CommandHistory {
    private val history = ArrayDeque<Command>()

    fun execute(command: Command) {
        command.execute()
        history.addLast(command)
    }

    fun undo() {
        history.removeLastOrNull()?.undo()
    }
}
```

**When to use:** Undo/redo, macro recording, task queues, transaction logging.

---

#### 16. Template Method
**Intent:** Define the skeleton of an algorithm in a base class; let subclasses override specific steps.

```kotlin
abstract class DataParser {
    // Template method — defines the algorithm skeleton
    fun parse(path: String): Data {
        val raw = readFile(path)
        val validated = validate(raw)
        return transform(validated)
    }

    protected open fun readFile(path: String): String = File(path).readText()
    protected abstract fun validate(raw: String): String
    protected abstract fun transform(validated: String): Data
}

class CsvParser : DataParser() {
    override fun validate(raw: String) = /* CSV validation */ raw
    override fun transform(validated: String) = /* parse CSV */ Data()
}
```

**When to use:** When multiple classes share the same algorithm structure but differ in specific steps.

---

#### 17. State
**Intent:** Allow an object to alter its behavior when its internal state changes.

```kotlin
sealed class OrderState {
    abstract fun next(order: Order): OrderState
    abstract fun cancel(order: Order): OrderState

    object Pending : OrderState() {
        override fun next(order: Order) = Confirmed
        override fun cancel(order: Order) = Cancelled
    }

    object Confirmed : OrderState() {
        override fun next(order: Order) = Shipped
        override fun cancel(order: Order) = Cancelled
    }

    object Shipped : OrderState() {
        override fun next(order: Order) = Delivered
        override fun cancel(order: Order) = throw IllegalStateException("Cannot cancel shipped order")
    }

    object Delivered : OrderState() {
        override fun next(order: Order) = throw IllegalStateException("Already delivered")
        override fun cancel(order: Order) = throw IllegalStateException("Cannot cancel delivered order")
    }

    object Cancelled : OrderState() {
        override fun next(order: Order) = throw IllegalStateException("Order is cancelled")
        override fun cancel(order: Order) = this
    }
}
```

**When to use:** Objects with distinct behavioral modes — order workflows, UI states, protocol handlers.

---

#### 18. Chain of Responsibility
**Intent:** Pass a request along a chain of handlers; each handler decides to process or pass it along.

```kotlin
abstract class Handler {
    var next: Handler? = null

    fun chain(handler: Handler): Handler {
        next = handler
        return handler
    }

    abstract fun handle(request: Request): Response?

    protected fun passToNext(request: Request): Response? = next?.handle(request)
}

class AuthHandler : Handler() {
    override fun handle(request: Request): Response? {
        if (!request.isAuthenticated) return Response(401, "Unauthorized")
        return passToNext(request)
    }
}

class RateLimitHandler : Handler() {
    override fun handle(request: Request): Response? {
        if (isRateLimited(request.ip)) return Response(429, "Too Many Requests")
        return passToNext(request)
    }
}
```

**When to use:** Middleware pipelines, request filtering, event processing chains.

---

#### 19. Mediator
**Intent:** Centralize complex communications between related objects.

```kotlin
class ChatRoom {
    private val users = mutableListOf<User>()

    fun join(user: User) { users.add(user) }

    fun sendMessage(from: User, message: String) {
        users.filter { it != from }
            .forEach { it.receive(from.name, message) }
    }
}
```

**When to use:** Chat systems, air traffic control, UI component coordination.

---

#### 20. Visitor
**Intent:** Add operations to object structures without modifying the classes.

```kotlin
sealed interface DocumentNode {
    fun <R> accept(visitor: DocumentVisitor<R>): R
}

data class TextNode(val content: String) : DocumentNode {
    override fun <R> accept(visitor: DocumentVisitor<R>) = visitor.visitText(this)
}

data class ImageNode(val url: String) : DocumentNode {
    override fun <R> accept(visitor: DocumentVisitor<R>) = visitor.visitImage(this)
}

interface DocumentVisitor<R> {
    fun visitText(node: TextNode): R
    fun visitImage(node: ImageNode): R
}

// Add new operations without changing node classes
class HtmlExporter : DocumentVisitor<String> {
    override fun visitText(node: TextNode) = "<p>${node.content}</p>"
    override fun visitImage(node: ImageNode) = """<img src="${node.url}"/>"""
}

class WordCounter : DocumentVisitor<Int> {
    override fun visitText(node: TextNode) = node.content.split(" ").size
    override fun visitImage(node: ImageNode) = 0
}
```

**When to use:** Compilers (AST traversal), document export, reporting across heterogeneous structures.

---

## Kotlin-Specific Language Features That Replace Patterns

| Pattern | Kotlin Alternative |
|---|---|
| Singleton | `object` keyword |
| Builder | Named arguments + default parameter values |
| Prototype | `data class` + `.copy()` |
| Decorator | `by` interface delegation |
| Strategy | Higher-order functions / lambdas |
| Observer | `Flow`, `StateFlow`, `SharedFlow` (coroutines) |
| State | `sealed class` + `when` (exhaustive) |
| Iterator | `Iterable`, `Sequence`, extension functions |
| Command | Lambdas / function references |

---

## Common Architectural Patterns

| Pattern | Purpose |
|---|---|
| **Repository** | Abstract data access behind a clean interface |
| **Service Layer** | Business logic separated from controllers/UI |
| **DAO** (Data Access Object) | Encapsulate persistence operations |
| **MVC / MVP / MVVM** | UI separation patterns |
| **Dependency Injection** | Supply dependencies externally (Dagger, Koin, Hilt) |
| **Event-Driven / Reactive** | Decouple producers from consumers |
| **CQRS** | Separate read and write models |
| **Domain-Driven Design** | Model business domain explicitly in code |

---

## Quick Reference: Choosing a Pattern

| Problem | Pattern(s) to consider |
|---|---|
| "I need exactly one instance" | Singleton |
| "I don't know which class to create until runtime" | Factory Method, Abstract Factory |
| "Object has too many constructor params" | Builder |
| "I need to add behavior without modifying a class" | Decorator, Visitor |
| "Two interfaces don't match" | Adapter |
| "Complex subsystem needs a simple API" | Facade |
| "I need to swap algorithms at runtime" | Strategy |
| "Object behavior changes based on state" | State |
| "I need undo/redo" | Command + Memento |
| "I need to notify multiple listeners of changes" | Observer |
| "Request goes through multiple processing steps" | Chain of Responsibility |
| "I need tree structures with uniform treatment" | Composite |
| "Too many similar objects eating memory" | Flyweight |
