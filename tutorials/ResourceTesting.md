# JUnit 5 Resource Usage Examples

## Working with Test Resources

### 1. Using @TempDir for File Operations

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class FileOperationTest {
    
    @Test
    void testFileCreation(@TempDir Path tempDir) throws IOException {
        // Create a test file in the temporary directory
        Path testFile = tempDir.resolve("test-file.txt");
        String content = "Hello JUnit 5 Testing!";
        
        Files.write(testFile, content.getBytes());
        
        assertTrue(Files.exists(testFile));
        assertEquals(content, Files.readString(testFile));
        
        // TempDir is automatically cleaned up after test
    }
    
    @Test
    void testDirectoryOperations(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        
        Path nestedFile = subDir.resolve("nested.txt");
        Files.write(nestedFile, "Nested content".getBytes());
        
        assertTrue(Files.isDirectory(subDir));
        assertTrue(Files.exists(nestedFile));
    }
}
```

### 2. Reading Resources from Classpath

```java
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ClasspathResourceTest {
    
    @Test
    void testReadJsonResource() throws IOException {
        // Reading from src/test/resources/data/test-data.json
        try (InputStream inputStream = getClass()
                .getResourceAsStream("/data/test-data.json")) {
            
            assertNotNull(inputStream, "Resource file should exist");
            
            String content = new String(inputStream.readAllBytes(), 
                StandardCharsets.UTF_8);
            assertFalse(content.isEmpty());
            assertTrue(content.contains("\"name\""));
        }
    }
    
    @Test
    void testReadPropertiesResource() throws IOException {
        Properties props = new Properties();
        
        try (InputStream inputStream = getClass()
                .getResourceAsStream("/config/test.properties")) {
            
            assertNotNull(inputStream);
            props.load(inputStream);
            
            assertEquals("test-value", props.getProperty("test.key"));
            assertEquals("5432", props.getProperty("database.port"));
        }
    }
    
    @Test
    void testReadCsvResource() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass()
                    .getResourceAsStream("/data/users.csv")))) {
            
            String headerLine = reader.readLine();
            assertNotNull(headerLine);
            assertTrue(headerLine.contains("name,email,age"));
            
            String firstDataLine = reader.readLine();
            assertNotNull(firstDataLine);
            String[] columns = firstDataLine.split(",");
            assertEquals(3, columns.length);
        }
    }
}
```

### 3. Database Testing with H2

```java
import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    
    private static Connection connection;
    
    @BeforeAll
    static void setUpDatabase() throws SQLException {
        // Create H2 in-memory database
        String jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        // Create test tables
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    age INT
                )
            """);
            
            stmt.execute("""
                CREATE TABLE orders (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT,
                    product VARCHAR(100),
                    amount DECIMAL(10,2),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
        }
    }
    
    @BeforeEach
    void insertTestData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Clear data before each test
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
            stmt.execute("ALTER TABLE orders ALTER COLUMN id RESTART WITH 1");
            
            // Insert test data
            stmt.execute("INSERT INTO users (name, email, age) VALUES " +
                "('John Doe', 'john@example.com', 30), " +
                "('Jane Smith', 'jane@example.com', 25)");
        }
    }
    
    @Test
    void testUserInsertion() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (name, email, age) VALUES (?, ?, ?)")) {
            
            stmt.setString(1, "Bob Wilson");
            stmt.setString(2, "bob@example.com");
            stmt.setInt(3, 35);
            
            int rowsAffected = stmt.executeUpdate();
            assertEquals(1, rowsAffected);
        }
        
        // Verify insertion
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1)); // 2 from setup + 1 from test
        }
    }
    
    @Test
    void testUserQuery() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name, email, age FROM users WHERE email = ?")) {
            
            stmt.setString(1, "john@example.com");
            
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("John Doe", rs.getString("name"));
                assertEquals(30, rs.getInt("age"));
                assertFalse(rs.next()); // Should be only one result
            }
        }
    }
    
    @Test
    void testJoinQuery() throws SQLException {
        // First insert an order
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO orders (user_id, product, amount) VALUES (1, 'Laptop', 999.99)")) {
            stmt.executeUpdate();
        }
        
        // Test join query
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                SELECT u.name, u.email, o.product, o.amount 
                FROM users u 
                JOIN orders o ON u.id = o.user_id 
                WHERE u.id = 1
            """)) {
            
            assertTrue(rs.next());
            assertEquals("John Doe", rs.getString("name"));
            assertEquals("Laptop", rs.getString("product"));
            assertEquals(999.99, rs.getDouble("amount"), 0.01);
        }
    }
    
    @AfterAll
    static void cleanUpDatabase() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
```

### 4. Testing with External Resources

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

class ExternalResourceTest {
    
    @Test
    @EnabledIfSystemProperty(named = "test.integration", matches = "true")
    void testExternalApiCall() {
        // This test only runs when -Dtest.integration=true is set
        // Example: integration test with external API
        
        ApiClient client = new ApiClient("https://api.example.com");
        Response response = client.get("/health");
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("healthy"));
    }
    
    @Test
    void testResourceValidation() {
        // Test that required resources exist
        String configPath = System.getProperty("config.path", "src/test/resources/config");
        File configDir = new File(configPath);
        
        assertTrue(configDir.exists(), "Config directory should exist");
        assertTrue(configDir.isDirectory(), "Config path should be a directory");
        
        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".properties"));
        assertNotNull(configFiles);
        assertTrue(configFiles.length > 0, "Should have at least one config file");
    }
}
```

## Resource Directory Structure

```
src/test/resources/
	data/
	   	test-data.json
	   	users.csv
	   	sample.xml
	config/
	   	test.properties
	   	database.properties
	sql/
	   	schema.sql
	   	test-data.sql
   	files/
   	   	sample.txt
   	   	binary-data.bin
```

## Best Practices for Resource Usage

1. **Use @TempDir for file operations** - Automatic cleanup, isolation between tests
2. **Store test data in resources directory** - Version controlled, easy to maintain
3. **Use relative paths for resources** - Platform independent
4. **Clean up external resources** - Use @AfterEach or @AfterAll for cleanup
5. **Mock external dependencies** - Use libraries like WireMock for external APIs
6. **Use in-memory databases** - H2, HSQLDB for database testing
7. **Conditional resource tests** - Skip tests when resources unavailable