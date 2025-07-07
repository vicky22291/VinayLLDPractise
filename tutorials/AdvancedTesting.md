# JUnit 5 Advanced Features - Dynamic Tests and Extensions

## Dynamic Tests Examples

### 1. Basic Dynamic Test Creation

```java
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.Assertions.*;

class DynamicTestExamples {
    
    @TestFactory
    @DisplayName("Dynamic Tests from Collection")
    Collection<DynamicTest> dynamicTestsFromCollection() {
        return Arrays.asList(
            dynamicTest("Addition Test", () -> assertEquals(2, 1 + 1)),
            dynamicTest("Multiplication Test", () -> assertEquals(6, 2 * 3)),
            dynamicTest("Division Test", () -> assertEquals(2, 4 / 2))
        );
    }
    
    @TestFactory
    @DisplayName("Dynamic Tests from Stream")
    Stream<DynamicTest> dynamicTestsFromStream() {
        return Stream.of("apple", "banana", "cherry")
            .map(fruit -> dynamicTest("Testing " + fruit, () -> {
                assertNotNull(fruit);
                assertTrue(fruit.length() > 3);
            }));
    }
    
    @TestFactory
    @DisplayName("Dynamic Math Tests")
    Stream<DynamicTest> dynamicMathTests() {
        // Test data: input1, input2, expectedSum, expectedProduct
        int[][] testData = {
            {1, 2, 3, 2},
            {3, 4, 7, 12},
            {5, 6, 11, 30},
            {-1, 1, 0, -1}
        };
        
        return Arrays.stream(testData)
            .map(data -> dynamicTest(
                String.format("Testing %d and %d", data[0], data[1]),
                () -> {
                    Calculator calc = new Calculator();
                    assertEquals(data[2], calc.add(data[0], data[1]), 
                        "Addition failed");
                    assertEquals(data[3], calc.multiply(data[0], data[1]), 
                        "Multiplication failed");
                }
            ));
    }
}
```

### 2. Dynamic Tests with External Data

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class DataDrivenDynamicTests {
    
    @TestFactory
    @DisplayName("CSV-driven Dynamic Tests")
    Stream<DynamicTest> csvDrivenTests() throws IOException {
        Path csvFile = Paths.get("src/test/resources/test-cases.csv");
        
        return Files.lines(csvFile)
            .skip(1) // Skip header
            .map(line -> line.split(","))
            .map(parts -> dynamicTest(
                "Testing scenario: " + parts[0],
                () -> {
                    String input = parts[1];
                    String expected = parts[2];
                    String actual = processInput(input);
                    assertEquals(expected, actual);
                }
            ));
    }
    
    @TestFactory
    @DisplayName("JSON-driven Dynamic Tests")
    Stream<DynamicTest> jsonDrivenTests() throws IOException {
        String jsonContent = Files.readString(
            Paths.get("src/test/resources/scenarios.json"));
        
        List<TestScenario> scenarios = objectMapper.readValue(
            jsonContent, 
            new TypeReference<List<TestScenario>>() {}
        );
        
        return scenarios.stream()
            .map(scenario -> dynamicTest(
                scenario.getName(),
                () -> executeScenario(scenario)
            ));
    }
    
    @TestFactory
    @DisplayName("Database-driven Dynamic Tests")
    Stream<DynamicTest> databaseDrivenTests() {
        List<TestCase> testCases = testCaseRepository.findAll();
        
        return testCases.stream()
            .map(testCase -> dynamicTest(
                "Test Case ID: " + testCase.getId(),
                () -> {
                    Object result = executeTestCase(testCase);
                    assertNotNull(result);
                    assertTrue(testCase.validate(result));
                }
            ));
    }
}
```

## Custom Extensions Examples

### 3. Timing Extension

```java
import org.junit.jupiter.api.extension.*;
import java.lang.reflect.Method;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private static final String START_TIME = "start time";
    
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }
    
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        Method testMethod = context.getRequiredTestMethod();
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.printf("Method [%s] took %d ms.%n", 
            testMethod.getName(), duration);
        
        // Optionally fail test if it takes too long
        if (testMethod.isAnnotationPresent(MaxExecutionTime.class)) {
            long maxTime = testMethod.getAnnotation(MaxExecutionTime.class).value();
            if (duration > maxTime) {
                throw new RuntimeException(
                    String.format("Test exceeded maximum execution time: %d ms (max: %d ms)", 
                        duration, maxTime));
            }
        }
    }
    
    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}

// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxExecutionTime {
    long value() default 1000; // milliseconds
}

// Usage
@ExtendWith(TimingExtension.class)
class TimedTests {
    
    @Test
    @MaxExecutionTime(500)
    void fastTest() throws InterruptedException {
        Thread.sleep(100); // Should pass
    }
    
    @Test
    @MaxExecutionTime(200)
    void slowTest() throws InterruptedException {
        Thread.sleep(300); // Should fail
    }
}
```

### 4. Database Transaction Extension

```java
import org.junit.jupiter.api.extension.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionalExtension implements BeforeEachCallback, AfterEachCallback {
    
    private static final String CONNECTION = "connection";
    private static final String AUTO_COMMIT = "auto_commit";
    
    private final DataSource dataSource;
    
    public TransactionalExtension(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Connection connection = dataSource.getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        
        connection.setAutoCommit(false);
        
        getStore(context).put(CONNECTION, connection);
        getStore(context).put(AUTO_COMMIT, originalAutoCommit);
    }
    
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Connection connection = getStore(context).get(CONNECTION, Connection.class);
        boolean originalAutoCommit = getStore(context).get(AUTO_COMMIT, boolean.class);
        
        try {
            // Always rollback to ensure test isolation
            connection.rollback();
            connection.setAutoCommit(originalAutoCommit);
        } finally {
            connection.close();
        }
    }
    
    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
    
    public static Connection getConnection(ExtensionContext context) {
        return context.getStore(Namespace.create(TransactionalExtension.class, 
            context.getRequiredTestMethod()))
            .get(CONNECTION, Connection.class);
    }
}

// Usage
class DatabaseTest {
    
    @RegisterExtension
    static TransactionalExtension transactionalExtension = 
        new TransactionalExtension(testDataSource);
    
    @Test
    void testDatabaseOperation(ExtensionContext context) throws SQLException {
        Connection conn = TransactionalExtension.getConnection(context);
        
        // Database operations here are automatically rolled back
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO users (name, email) VALUES (?, ?)");
        stmt.setString(1, "John Doe");
        stmt.setString(2, "john@example.com");
        stmt.executeUpdate();
        
        // Verify insertion
        ResultSet rs = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM users WHERE name = 'John Doe'");
        rs.next();
        assertEquals(1, rs.getInt(1));
        
        // Changes will be rolled back after test
    }
}
```

### 5. Parameter Resolution Extension

```java
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RandomDataExtension implements ParameterResolver {
    
    private final Random random = new Random();
    
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, 
                                   ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(RandomData.class);
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, 
                                 ExtensionContext extensionContext) {
        
        RandomData annotation = parameterContext.findAnnotation(RandomData.class).get();
        Class<?> type = parameterContext.getParameter().getType();
        
        if (type == String.class) {
            return generateRandomString(annotation.length());
        } else if (type == Integer.class || type == int.class) {
            return random.nextInt(annotation.max() - annotation.min() + 1) + annotation.min();
        } else if (type == User.class) {
            return new User(
                generateRandomString(10),
                generateRandomString(5) + "@example.com"
            );
        }
        
        throw new ParameterResolutionException(
            "Unsupported parameter type: " + type.getName());
    }
    
    private String generateRandomString(int length) {
        return random.ints(97, 123) // a-z
            .limit(length)
            .collect(StringBuilder::new, 
                    StringBuilder::appendCodePoint, 
                    StringBuilder::append)
            .toString();
    }
}

// Custom annotation
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RandomData {
    int length() default 10;
    int min() default 0;
    int max() default 100;
}

// Usage
@ExtendWith(RandomDataExtension.class)
class RandomDataTest {
    
    @Test
    void testWithRandomString(@RandomData(length = 15) String randomString) {
        assertNotNull(randomString);
        assertEquals(15, randomString.length());
    }
    
    @Test
    void testWithRandomNumber(@RandomData(min = 10, max = 50) int randomNumber) {
        assertTrue(randomNumber >= 10);
        assertTrue(randomNumber <= 50);
    }
    
    @Test
    void testWithRandomUser(@RandomData User randomUser) {
        assertNotNull(randomUser);
        assertNotNull(randomUser.getName());
        assertTrue(randomUser.getEmail().contains("@example.com"));
    }
}
```

### 6. Conditional Extension

```java
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnvironmentCondition implements ExecutionCondition {
    
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        
        if (context.getElement().isPresent()) {
            RequiresEnvironment annotation = context.getElement().get()
                .getAnnotation(RequiresEnvironment.class);
                
            if (annotation != null) {
                String requiredEnv = annotation.value();
                String currentEnv = System.getProperty("test.environment", "default");
                
                if (requiredEnv.equals(currentEnv)) {
                    return ConditionEvaluationResult.enabled(
                        "Test enabled for environment: " + currentEnv);
                } else {
                    return ConditionEvaluationResult.disabled(
                        String.format("Test requires environment '%s' but current is '%s'", 
                            requiredEnv, currentEnv));
                }
            }
        }
        
        return ConditionEvaluationResult.enabled("No environment restriction");
    }
}

// Custom annotation
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnvironmentCondition.class)
public @interface RequiresEnvironment {
    String value();
}

// Usage
class EnvironmentSpecificTests {
    
    @Test
    @RequiresEnvironment("development")
    void developmentOnlyTest() {
        // Only runs when -Dtest.environment=development
    }
    
    @Test
    @RequiresEnvironment("production")
    void productionOnlyTest() {
        // Only runs when -Dtest.environment=production
    }
    
    @Test
    void alwaysRunsTest() {
        // Runs in any environment
    }
}
```

## Best Practices for Dynamic Tests and Extensions

### Dynamic Tests Best Practices:
1. **Use meaningful test names** - Include context about what's being tested
2. **Keep test logic simple** - Each dynamic test should focus on one scenario
3. **Handle failures gracefully** - Ensure one failing dynamic test doesn't affect others
4. **Document data sources** - Make it clear where test data comes from
5. **Consider performance** - Large datasets can create many tests

### Extensions Best Practices:
1. **Single responsibility** - Each extension should have one clear purpose
2. **Proper cleanup** - Always clean up resources in appropriate lifecycle methods
3. **Thread safety** - Extensions may be used in parallel execution
4. **Error handling** - Provide clear error messages when extensions fail
5. **Documentation** - Document extension behavior and usage patterns