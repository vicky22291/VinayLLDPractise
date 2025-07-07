# JUnit 5 with Mockito - Mocking Examples

## Setup Dependencies

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.5.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.5.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Gradle

```gradle
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
testImplementation 'org.mockito:mockito-core:5.5.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.5.0'
```

## Basic Mocking Examples

### 1. Using @ExtendWith(MockitoExtension.class)

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testCreateUser() {
        // Arrange
        User newUser = new User("john@example.com", "John Doe");
        User savedUser = new User(1L, "john@example.com", "John Doe");
        
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(emailService.sendWelcomeEmail(savedUser.getEmail())).thenReturn(true);
        
        // Act
        User result = userService.createUser(newUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
        
        verify(userRepository).save(newUser);
        verify(emailService).sendWelcomeEmail("john@example.com");
        verifyNoMoreInteractions(userRepository, emailService);
    }
    
    @Test
    void testFindUserById() {
        // Arrange
        Long userId = 1L;
        User expectedUser = new User(userId, "jane@example.com", "Jane Smith");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
        
        // Act
        Optional<User> result = userService.findById(userId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedUser, result.get());
        
        verify(userRepository).findById(userId);
    }
    
    @Test
    void testDeleteUser() {
        // Arrange
        Long userId = 1L;
        User user = new User(userId, "test@example.com", "Test User");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(userId);
        
        // Act
        boolean result = userService.deleteUser(userId);
        
        // Assert
        assertTrue(result);
        
        verify(userRepository).findById(userId);
        verify(userRepository).deleteById(userId);
    }
}
```

### 2. Manual Mock Creation

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    
    private PaymentGateway paymentGateway;
    private NotificationService notificationService;
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        // Manual mock creation
        paymentGateway = mock(PaymentGateway.class);
        notificationService = mock(NotificationService.class);
        paymentService = new PaymentService(paymentGateway, notificationService);
    }
    
    @Test
    void testSuccessfulPayment() {
        // Arrange
        PaymentRequest request = new PaymentRequest("123", 100.0, "USD");
        PaymentResponse expectedResponse = new PaymentResponse("SUCCESS", "txn_123");
        
        when(paymentGateway.processPayment(request)).thenReturn(expectedResponse);
        doNothing().when(notificationService).sendPaymentConfirmation(anyString(), any());
        
        // Act
        PaymentResult result = paymentService.processPayment(request);
        
        // Assert
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("txn_123", result.getTransactionId());
        
        verify(paymentGateway).processPayment(request);
        verify(notificationService).sendPaymentConfirmation(eq("123"), any());
    }
}
```

## Advanced Mocking Scenarios

### 3. Argument Matchers and Captors

```java
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private InventoryService inventoryService;
    
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void testCreateOrderWithArgumentCaptor() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(2);
        
        when(inventoryService.isAvailable(100L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(999L);
            return order;
        });
        
        // Act
        Order result = orderService.createOrder(request);
        
        // Assert
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        
        assertEquals(1L, capturedOrder.getCustomerId());
        assertEquals(100L, capturedOrder.getProductId());
        assertEquals(2, capturedOrder.getQuantity());
        assertNotNull(capturedOrder.getCreatedAt());
    }
    
    @Test
    void testWithArgumentMatchers() {
        // Using various argument matchers
        when(inventoryService.isAvailable(anyLong(), gt(0))).thenReturn(true);
        when(inventoryService.isAvailable(anyLong(), eq(0))).thenReturn(false);
        
        // Test with positive quantity
        assertTrue(orderService.checkAvailability(100L, 5));
        
        // Test with zero quantity
        assertFalse(orderService.checkAvailability(100L, 0));
        
        verify(inventoryService).isAvailable(eq(100L), gt(0));
        verify(inventoryService).isAvailable(eq(100L), eq(0));
    }
}
```

### 4. Stubbing Void Methods and Exceptions

```java
@ExtendWith(MockitoExtension.class)
class FileProcessorTest {
    
    @Mock
    private FileReader fileReader;
    
    @Mock
    private DataValidator validator;
    
    @InjectMocks
    private FileProcessor fileProcessor;
    
    @Test
    void testProcessFileSuccess() throws Exception {
        // Arrange
        String filePath = "/test/file.csv";
        List<String> fileContent = Arrays.asList("header", "data1", "data2");
        
        when(fileReader.readLines(filePath)).thenReturn(fileContent);
        doNothing().when(validator).validate(anyString());
        
        // Act
        ProcessResult result = fileProcessor.processFile(filePath);
        
        // Assert
        assertEquals(ProcessStatus.SUCCESS, result.getStatus());
        assertEquals(2, result.getProcessedLines()); // Excluding header
        
        verify(fileReader).readLines(filePath);
        verify(validator, times(2)).validate(anyString());
    }
    
    @Test
    void testProcessFileWithValidationError() throws Exception {
        // Arrange
        String filePath = "/test/invalid-file.csv";
        List<String> fileContent = Arrays.asList("header", "invalid-data");
        
        when(fileReader.readLines(filePath)).thenReturn(fileContent);
        doThrow(new ValidationException("Invalid data format"))
            .when(validator).validate("invalid-data");
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> fileProcessor.processFile(filePath));
        
        assertEquals("Invalid data format", exception.getMessage());
        
        verify(fileReader).readLines(filePath);
        verify(validator).validate("invalid-data");
    }
    
    @Test
    void testProcessFileWithIOException() throws Exception {
        // Arrange
        String filePath = "/nonexistent/file.csv";
        
        when(fileReader.readLines(filePath))
            .thenThrow(new IOException("File not found"));
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, 
            () -> fileProcessor.processFile(filePath));
        
        assertEquals("File not found", exception.getMessage());
    }
}
```

### 5. Spies and Partial Mocking

```java
@ExtendWith(MockitoExtension.class)
class CalculatorServiceTest {
    
    @Spy
    private MathUtils mathUtils;
    
    @InjectMocks
    private CalculatorService calculatorService;
    
    @Test
    void testWithSpy() {
        // Spy calls real methods by default, but you can stub specific methods
        doReturn(100.0).when(mathUtils).complexCalculation(anyDouble());
        
        // This will call the real method
        double realResult = mathUtils.add(5.0, 3.0);
        assertEquals(8.0, realResult);
        
        // This will return the stubbed value
        double stubbedResult = calculatorService.performComplexOperation(10.0);
        assertEquals(100.0, stubbedResult);
        
        verify(mathUtils).add(5.0, 3.0);
        verify(mathUtils).complexCalculation(10.0);
    }
}
```

### 6. Testing with Multiple Mocks and Behavior Verification

```java
@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {
    
    @Mock
    private ProductService productService;
    
    @Mock
    private PricingService pricingService;
    
    @Mock
    private DiscountService discountService;
    
    @InjectMocks
    private ShoppingCartService cartService;
    
    @Test
    void testAddItemToCart() {
        // Arrange
        Long productId = 1L;
        int quantity = 2;
        Product product = new Product(productId, "Test Product", 50.0);
        
        when(productService.findById(productId)).thenReturn(product);
        when(pricingService.calculatePrice(product, quantity)).thenReturn(100.0);
        when(discountService.getDiscount(any(), anyDouble())).thenReturn(10.0);
        
        // Act
        CartItem result = cartService.addItem(productId, quantity);
        
        // Assert
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertEquals(quantity, result.getQuantity());
        assertEquals(90.0, result.getTotalPrice()); // 100.0 - 10.0 discount
        
        InOrder inOrder = inOrder(productService, pricingService, discountService);
        inOrder.verify(productService).findById(productId);
        inOrder.verify(pricingService).calculatePrice(product, quantity);
        inOrder.verify(discountService).getDiscount(any(), eq(100.0));
    }
    
    @Test
    void testCheckoutProcess() {
        // Complex scenario with multiple service interactions
        Cart cart = new Cart();
        cart.addItem(new CartItem(1L, 2, 100.0));
        cart.addItem(new CartItem(2L, 1, 50.0));
        
        when(pricingService.calculateTotalPrice(cart)).thenReturn(150.0);
        when(discountService.applyPromotions(cart)).thenReturn(15.0);
        
        CheckoutResult result = cartService.checkout(cart);
        
        assertEquals(135.0, result.getFinalAmount());
        
        verify(pricingService).calculateTotalPrice(cart);
        verify(discountService).applyPromotions(cart);
        verifyNoMoreInteractions(productService); // Should not be called during checkout
    }
}
```

## Best Practices for Mocking with JUnit 5

1. **Use @ExtendWith(MockitoExtension.class)** for cleaner setup
2. **Prefer @Mock over manual mock()** calls for better readability
3. **Use @InjectMocks** for automatic dependency injection
4. **Verify interactions** with mocks to ensure proper behavior
5. **Use ArgumentCaptor** to verify complex arguments
6. **Stub only what's necessary** for each test
7. **Use @Spy** for partial mocking when needed
8. **Order verification** with InOrder for sequence-dependent operations
9. **Reset mocks** between tests if needed (rarely required with JUnit 5)
10. **Mock external dependencies** but avoid over-mocking