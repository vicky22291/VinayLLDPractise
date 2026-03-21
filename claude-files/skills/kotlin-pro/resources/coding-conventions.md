# Kotlin Coding Conventions

## 1. Naming Conventions

### Classes & Interfaces

| Entity | Convention | Example |
|--------|-----------|---------|
| Class | PascalCase, noun | `OrderProcessor`, `UserRepository` |
| Interface | PascalCase, adjective or noun (no `I` prefix) | `Serializable`, `PaymentGateway` |
| Abstract class | PascalCase, `Base`/`Abstract` prefix only if needed for clarity | `BaseRepository` only if `Repository` interface exists |
| Sealed class/interface | PascalCase, represents the type family | `PaymentResult`, `ScreenState` |
| Data class | PascalCase, noun — the thing it holds | `UserProfile`, `OrderSummary` |
| Object (singleton) | PascalCase, noun | `DatabaseConfig`, `AppMetrics` |
| Companion object factory | Verb phrase | `Connection.create()`, `User.fromJson()` |
| Enum | PascalCase class, SCREAMING_SNAKE values | `enum class Status { ACTIVE, INACTIVE }` |

### Functions

| Type | Convention | Example |
|------|-----------|---------|
| Action | Verb phrase | `processOrder()`, `sendNotification()` |
| Query / getter | Noun or `get` prefix if ambiguous | `totalPrice()`, `getUserById()` |
| Boolean-returning | `is`/`has`/`can`/`should` prefix | `isEligible()`, `hasPermission()`, `canRetry()` |
| Factory | `create`/`of`/`from` prefix | `createFrom()`, `of()`, `fromJson()` |
| Converter | `toXxx` | `toDto()`, `toEntity()`, `toDomainModel()` |
| Callback / handler | `on` prefix | `onPaymentReceived()`, `onError()` |
| Suspend | Same as above — don't suffix with `Async` | `fetchUser()` not `fetchUserAsync()` |
| Extension | Read as natural English | `String.isValidEmail()`, `List<Order>.totalValue()` |

### Variables & Properties

| Type | Convention | Example |
|------|-----------|---------|
| Local / property | camelCase, descriptive noun | `activeUsers`, `orderTotal` |
| Boolean | `is`/`has`/`should` prefix | `val isActive`, `val hasExpired` |
| Collection | Plural noun | `val users`, `val pendingOrders` |
| Map | `xxxByYyy` or descriptive | `val userById`, `val priceByProductId` |
| Constant (`const val`) | SCREAMING_SNAKE_CASE | `const val MAX_RETRIES = 3` |
| Top-level val (non-const) | camelCase | `val defaultTimeout = 30.seconds` |
| Backing property | Underscore prefix | `private val _state` backs `val state` |
| Lambda parameter | Descriptive, not `it` if body > 1 line | `users.filter { user -> user.isActive }` |

### Packages

```
com.company.project.module    // all lowercase, no underscores
com.company.project.data.repository
com.company.project.domain.model
com.company.project.service
```

### Test Methods

```kotlin
// Use backtick names — reads as a sentence
@Test
fun `should reject order when inventory is insufficient`() { }

@Test
fun `returns empty list when no users match criteria`() { }

// Naming pattern: `should {expected behavior} when {condition}`
// or: `{action} {expected result} {condition}`
```

### Anti-Patterns

```kotlin
// BAD: abbreviations
val usrMgr = UserManager()      // GOOD: val userManager
val cnt = orders.size            // GOOD: val orderCount
val res = api.fetch()            // GOOD: val response

// BAD: type in the name
val userList = listOf<User>()    // GOOD: val users
val nameString = "Alice"         // GOOD: val name
val isActiveBoolean = true       // GOOD: val isActive

// BAD: generic names
val data = fetch()               // GOOD: val userProfile = fetchUserProfile()
val result = process()           // GOOD: val validatedOrder = validateOrder()
val temp = calculate()           // GOOD: val discountedPrice = calculateDiscount()

// BAD: negated booleans (causes double-negation in conditions)
val isNotValid = false           // GOOD: val isValid = true
if (!isNotValid) { }             // GOOD: if (isValid) { }
```

---

## 2. Documentation Conventions

### When to Document

| Situation | Document? | How |
|-----------|-----------|-----|
| Class purpose | Always | One-sentence KDoc on every public class |
| Public API function | Yes, if non-obvious | KDoc with purpose, not mechanics |
| Internal/private function | Only if tricky | Inline comment explaining why |
| Business rule | Always | Inline comment at the decision point |
| Flow transition | Always | Inline comment before the next logical step |
| Workaround / hack | Always | `// WORKAROUND:` with link to issue |
| TODO | Yes | `// TODO(owner): description — JIRA-123` |
| Obvious code | Never | Let names speak |

### Class Documentation

```kotlin
/** Manages user session lifecycle from login through expiration and renewal. */
class SessionManager(
    private val tokenService: TokenService,
    private val sessionStore: SessionStore,
)

/** Immutable snapshot of a user's profile at a point in time. */
data class UserProfile(
    val id: UserId,
    val displayName: String,
    val email: Email,
    val tier: SubscriptionTier,
)

// NO doc needed — sealed variants are self-documenting
sealed interface CheckoutResult {
    data class Success(val orderId: OrderId) : CheckoutResult
    data class PaymentFailed(val reason: String) : CheckoutResult
    data object CartEmpty : CheckoutResult
}
```

### Function Documentation

```kotlin
// GOOD: doc explains non-obvious behavior
/**
 * Reserves inventory for the given items. Reservation expires after [ttl]
 * if not confirmed via [confirmReservation]. Thread-safe.
 */
suspend fun reserveInventory(
    items: List<LineItem>,
    ttl: Duration = 15.minutes,
): Reservation

// GOOD: no doc needed — name + types tell the whole story
suspend fun findUserById(id: UserId): User?

// GOOD: doc for surprising behavior only
/** Returns cached result if available and less than [maxAge] old. */
suspend fun getExchangeRate(from: Currency, to: Currency, maxAge: Duration = 5.minutes): BigDecimal
```

### Flow Comments (the core documentation technique)

```kotlin
suspend fun processRefund(orderId: OrderId): RefundResult {
    val order = orderRepository.findById(orderId)
        ?: return RefundResult.OrderNotFound

    // Only completed orders can be refunded — pending orders should be cancelled instead
    if (order.status != OrderStatus.COMPLETED) {
        return RefundResult.NotEligible(order.status)
    }

    // Capture the refund with the payment provider before updating our records
    // to ensure we never mark an order as refunded without money actually moving
    val providerRefund = paymentGateway.refund(order.paymentId, order.total)

    // Update order status and notify — these are best-effort,
    // the refund is already committed at the provider level
    orderRepository.updateStatus(orderId, OrderStatus.REFUNDED)
    eventBus.publish(OrderRefunded(orderId, providerRefund.id))

    return RefundResult.Success(providerRefund.id)
}
```

### What NOT to Document

```kotlin
// BAD: restates the code
// Get the user by ID
val user = userRepository.findById(id)

// BAD: documents the obvious
// Check if user is null
if (user == null) return

// BAD: changelog in comments
// Added by John on 2024-01-15 — JIRA-456
// Modified by Alice on 2024-03-20 — JIRA-789

// BAD: section dividers
// ==================== PRIVATE METHODS ====================
```

---

## 3. Collections, Sequences & Streams

### Iterable (List/Set) vs Sequence

| Use `List` chains when | Use `Sequence` when |
|------------------------|---------------------|
| Collection is small (< 1000 elements) | Collection is large or unbounded |
| You need the full result | You need only first N results |
| Chain has 1-2 operations | Chain has 3+ operations (avoids intermediate lists) |
| Debugging matters (eager = easier to inspect) | Performance matters (lazy = fewer allocations) |

```kotlin
// Small list — use direct chain (creates intermediate lists, but negligible for small N)
val activeEmails = users
    .filter { it.isActive }
    .map { it.email }

// Large list or long chain — use Sequence (lazy, single pass)
val firstTenPremiumNames = users.asSequence()
    .filter { it.tier == Tier.PREMIUM }
    .filter { it.isActive }
    .map { it.displayName }
    .take(10)
    .toList()  // terminal operation triggers evaluation

// File processing — inherently sequential, use Sequence
val errorLines = file.useLines { lines ->
    lines.filter { it.contains("ERROR") }
        .map { it.trim() }
        .toList()
}
```

### Flow (Async Streams)

```kotlin
// Use Flow for async data streams — the coroutine equivalent of Sequence
fun observeOrderUpdates(orderId: OrderId): Flow<OrderStatus> = flow {
    while (currentCoroutineContext().isActive) {
        val status = orderRepository.getStatus(orderId)
        emit(status)
        if (status.isTerminal) break
        delay(5.seconds)
    }
}

// Collecting with operators
orderUpdates
    .filter { it != OrderStatus.PENDING }
    .distinctUntilChanged()
    .onEach { logger.info { "Order $orderId status: $it" } }
    .collect { updateUi(it) }

// Flow vs Channel
// Flow: cold stream, one collector, declarative — prefer this
// Channel: hot stream, multiple consumers, imperative — use for fan-out
```

### Common Collection Operations

```kotlin
// Grouping
val ordersByStatus: Map<OrderStatus, List<Order>> = orders.groupBy { it.status }

// Associating (1:1 mapping)
val userById: Map<UserId, User> = users.associateBy { it.id }

// Partitioning (binary split)
val (active, inactive) = users.partition { it.isActive }

// Folding / reducing
val totalRevenue = orders.fold(BigDecimal.ZERO) { acc, order -> acc + order.total }

// Flat mapping nested structures
val allLineItems = orders.flatMap { it.lineItems }

// Windowed / chunked (batch processing)
users.chunked(100).forEach { batch ->
    userRepository.saveAll(batch)
}

// Zipping parallel lists
val pairs: List<Pair<Question, Answer>> = questions.zip(answers)

// First / single with meaningful errors
val admin = users.firstOrNull { it.role == Role.ADMIN }
    ?: error("No admin user configured")

val onlyOwner = users.singleOrNull { it.role == Role.OWNER }
    ?: error("Expected exactly one owner, found ${users.count { it.role == Role.OWNER }}")
```

### Prefer Collection Operations Over Loops

```kotlin
// BAD: imperative loop
val result = mutableListOf<String>()
for (user in users) {
    if (user.isActive) {
        result.add(user.email)
    }
}

// GOOD: functional chain
val result = users.filter { it.isActive }.map { it.email }

// EXCEPTION: complex multi-step logic with early exits — loops are clearer
// Use buildList when you need loop + conditional add
val notifications = buildList {
    for (user in users) {
        val pref = preferenceService.get(user.id) ?: continue
        if (!pref.emailEnabled) continue
        add(Notification(user.email, pref.frequency))
    }
}
```

### Map Operations

```kotlin
// getOrPut for cache-like maps
val cached = cache.getOrPut(key) { expensiveComputation() }

// mapValues / mapKeys for transforming maps
val nameLengths: Map<UserId, Int> = userById.mapValues { (_, user) -> user.name.length }

// filterValues / filterKeys
val activeUsers = userById.filterValues { it.isActive }

// Merging maps
val merged = map1 + map2  // map2 values win on conflict

// Building maps from scratch
val lookup = buildMap {
    put("default", defaultConfig)
    configs.forEach { put(it.name, it) }
}
```

### Pitfalls to Avoid

```kotlin
// BAD: calling .toList() in the middle of a Sequence chain (defeats laziness)
val result = users.asSequence()
    .filter { it.isActive }
    .toList()                    // <-- unnecessary materialization
    .map { it.email }

// BAD: using Sequence for small collections (overhead > benefit)
val names = listOf("a", "b", "c").asSequence().map { it.uppercase() }.toList()

// BAD: mutating a collection while iterating
// Use .toList() to snapshot, or use iterator.remove()
for (item in mutableList) {
    if (item.isExpired) mutableList.remove(item)  // ConcurrentModificationException
}
// GOOD:
mutableList.removeAll { it.isExpired }

// BAD: chaining .filter{}.first{} — use .first{} with combined predicate
val match = users.filter { it.isActive }.first { it.age > 21 }
// GOOD:
val match = users.first { it.isActive && it.age > 21 }

// BAD: .map{}.filterNotNull() — use mapNotNull
val emails = users.map { it.email }.filterNotNull()
// GOOD:
val emails = users.mapNotNull { it.email }
```
