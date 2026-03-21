# Kotlin Logging Best Practices

## Setup

```kotlin
// build.gradle.kts
implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
implementation("ch.qos.logback:logback-classic:1.5.6")
```

## Logger Declaration

Always declare at file level (makes field static, avoids per-instance overhead):

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class OrderService {
    fun process(order: Order) {
        logger.info { "Processing order ${order.id}" }
    }
}
```

## Lazy Evaluation (Critical)

```kotlin
// GOOD: lambda evaluated only if level is enabled
logger.debug { "Item details: ${item.toDetailedString()}" }

// BAD: string always constructed regardless of level
logger.debug("Item details: ${item.toDetailedString()}")
```

## Exception Logging

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    logger.error(e) { "Failed to process order ${order.id}" }
}
```

## Structured Logging with MDC

```kotlin
import io.github.oshai.kotlinlogging.withLoggingContext

suspend fun processRequest(userId: String, requestId: String) {
    withLoggingContext("userId" to userId, "requestId" to requestId) {
        logger.info { "Processing request" }
        doWork()
    }
}
```

## Fluent API (kotlin-logging 7.x)

```kotlin
logger.atWarn {
    message = "Rate limit exceeded"
    cause = exception
    payload = buildMap {
        put("endpoint", "/api/users")
        put("clientIp", request.remoteAddr)
    }
}
```

## Log Level Guidelines

| Level | Use for |
|-------|---------|
| ERROR | Unrecoverable failures needing immediate attention |
| WARN  | Unexpected but recoverable situations |
| INFO  | Key business events, startup/shutdown, state transitions |
| DEBUG | Detailed diagnostic info for development |
| TRACE | Very fine-grained, typically only in libraries |

## Rules

1. **One logger per file**, declared at file level
2. **Always use lambda syntax** for log messages (lazy evaluation)
3. **Log at boundaries**: entry/exit of services, external calls, state transitions
4. **Include identifiers**: order IDs, user IDs, request IDs in messages
5. **Use MDC** for cross-cutting context (request tracing, user sessions)
6. **Never log sensitive data**: passwords, tokens, PII
7. **Never log inside tight loops** — aggregate or sample instead
8. **ERROR = needs action**, WARN = investigate later, INFO = audit trail
9. **Log the "why"** not the "what" — `"Order rejected: insufficient inventory"` not `"Calling rejectOrder()"`
10. **Coroutine-aware**: Use `withLoggingContext` instead of raw MDC in suspend functions
