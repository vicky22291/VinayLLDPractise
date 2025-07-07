# JUnit 5 Basic Testing Example

```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@DisplayName("Calculator Test Suite")
class CalculatorTest {
    
    private Calculator calculator;
    
    @BeforeAll
    static void setUpAll() {
        System.out.println("Setting up test environment");
    }
    
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }
    
    @Test
    @DisplayName("Addition should work correctly")
    @Tag("fast")
    void testAddition() {
        assertEquals(5, calculator.add(2, 3), 
            "Addition of 2 + 3 should equal 5");
    }
    
    @Test
    @DisplayName("Division by zero should throw exception")
    void testDivisionByZero() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class, 
            () -> calculator.divide(10, 0),
            "Division by zero should throw ArithmeticException"
        );
        assertEquals("Division by zero", exception.getMessage());
    }
    
    @ParameterizedTest
    @DisplayName("Multiple addition scenarios")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void testMultipleAdditions(int value) {
        assertTrue(calculator.add(value, 1) > value);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1, 1, 2",
        "2, 3, 5", 
        "5, 7, 12",
        "-1, 1, 0"
    })
    void testAdditionWithCsv(int first, int second, int expected) {
        assertEquals(expected, calculator.add(first, second));
    }
    
    @RepeatedTest(value = 5, name = "Repetition {currentRepetition} of {totalRepetitions}")
    void testRepeatedCalculation() {
        assertEquals(4, calculator.multiply(2, 2));
    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testWithTimeout() {
        // This test must complete within 2 seconds
        assertTimeout(Duration.ofSeconds(1), () -> {
            calculator.complexCalculation();
        });
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testOnlyOnWindows() {
        // This test only runs on Windows
        assertTrue(calculator.isValid());
    }
    
    @Test
    @DisabledIfSystemProperty(named = "test.environment", matches = "production")
    void testNotInProduction() {
        // This test is disabled in production environment
        calculator.dangerousOperation();
    }
    
    @Test
    void testWithAssumptions() {
        assumeTrue("CI".equals(System.getenv("ENV")));
        // Test only runs in CI environment
        assertEquals(0, calculator.subtract(5, 5));
    }
    
    @AfterEach
    void tearDown() {
        calculator = null;
    }
    
    @AfterAll
    static void tearDownAll() {
        System.out.println("Cleaning up test environment");
    }
}
```

## Key Features Demonstrated:

1. **Lifecycle Methods**: `@BeforeAll`, `@BeforeEach`, `@AfterEach`, `@AfterAll`
2. **Basic Testing**: `@Test` with assertions
3. **Display Names**: Custom test names for better readability
4. **Tags**: Organizing tests with `@Tag`
5. **Exception Testing**: Using `assertThrows()`
6. **Parameterized Tests**: `@ValueSource` and `@CsvSource`
7. **Repeated Tests**: Running same test multiple times
8. **Timeouts**: Testing execution time constraints
9. **Conditional Execution**: OS-specific and property-based conditions
10. **Assumptions**: Conditional test execution based on environment