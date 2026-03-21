# Test Planning Examples

## Example 1: Pure Function Test Plan

**Code**:
```
function calculate_discount(price, discount_percent):
    if price < 0 or discount_percent < 0 or discount_percent > 100:
        raise ValueError("Invalid input")
    return price * (discount_percent / 100)
```

**Test Plan**:
```markdown
# Test Plan: calculate_discount

## Code Summary
- Purpose: Calculate discount amount from price and percentage
- Type: Pure Function
- Dependencies: None

## Test Strategy
- Unit Tests: 8 scenarios
- Coverage Target: 100%

## Mocking Strategy
None (pure function)

## Test Scenarios

### Scenario 1: Valid inputs - normal case
**Type**: Unit
**Input**: price=100, discount_percent=10
**Expected**: Returns 10.0
**Assertions**: result == 10.0

### Scenario 2: Valid inputs - zero discount
**Type**: Unit
**Input**: price=100, discount_percent=0
**Expected**: Returns 0.0
**Assertions**: result == 0.0

### Scenario 3: Valid inputs - 100% discount
**Type**: Unit
**Input**: price=100, discount_percent=100
**Expected**: Returns 100.0
**Assertions**: result == 100.0

### Scenario 4: Edge case - zero price
**Type**: Unit
**Input**: price=0, discount_percent=10
**Expected**: Returns 0.0
**Assertions**: result == 0.0

### Scenario 5: Error - negative price
**Type**: Unit
**Input**: price=-100, discount_percent=10
**Expected**: Raises ValueError
**Assertions**: pytest.raises(ValueError)

### Scenario 6: Error - negative discount
**Type**: Unit
**Input**: price=100, discount_percent=-10
**Expected**: Raises ValueError
**Assertions**: pytest.raises(ValueError)

### Scenario 7: Error - discount > 100
**Type**: Unit
**Input**: price=100, discount_percent=150
**Expected**: Raises ValueError
**Assertions**: pytest.raises(ValueError)

### Scenario 8: Edge case - large numbers
**Type**: Unit
**Input**: price=999999.99, discount_percent=99.99
**Expected**: Returns ~999899.99
**Assertions**: pytest.approx(result, 999899.99, rel=0.01)

## Success Criteria
- [x] All 8 scenarios covered
- [x] Edge cases: zero, boundaries tested
- [x] Error conditions: all validation errors tested
- [x] Coverage: 100% (simple function)
```

---

## Example 2: Class with Dependencies Test Plan

**Code**:
```
class OrderProcessor:
    def __init__(self, db, payment_gateway):
        self.db = db
        self.payment_gateway = payment_gateway

    def process_order(self, order_id):
        order = self.db.get_order(order_id)
        if not order:
            raise OrderNotFoundError(order_id)
        if order.status != "pending":
            raise InvalidOrderStateError(order.status)
        payment_result = self.payment_gateway.charge(order.total)
        if payment_result.success:
            order.status = "completed"
            self.db.save(order)
            return True
        else:
            order.status = "failed"
            self.db.save(order)
            return False
```

**Test Plan**:
```markdown
# Test Plan: OrderProcessor

## Code Summary
- Purpose: Process orders with payment and database updates
- Type: Stateful Class with External Dependencies
- Dependencies: Database, Payment Gateway

## Test Strategy
- Unit Tests: 6 scenarios (all dependencies mocked)
- Integration Tests: 2 scenarios (real DB, mocked payment)
- Coverage Target: 90%

## Mocking Strategy

**Database (self.db)**:
- Approach: Mock
- Methods to mock: get_order(), save()
- Verify: save() called with correct order state

**Payment Gateway (self.payment_gateway)**:
- Approach: Mock
- Methods to mock: charge()
- Verify: charge() called with correct amount

## Test Scenarios (Unit Tests)

### Scenario 1: Successful order processing
**Setup**: Mock db.get_order() returns pending order, mock payment.charge() returns success
**Input**: order_id=123
**Expected**: Returns True, order status = "completed", db.save() called
**Assertions**: result == True, mock_db.save.called_once(), saved_order.status == "completed"

### Scenario 2: Order not found
**Setup**: Mock db.get_order() returns None
**Expected**: Raises OrderNotFoundError
**Assertions**: pytest.raises(OrderNotFoundError, match="999")

### Scenario 3: Order in wrong state
**Setup**: Mock db.get_order() returns order with status="completed"
**Expected**: Raises InvalidOrderStateError

### Scenario 4: Payment failure
**Setup**: Mock payment.charge() returns failure
**Expected**: Returns False, order status = "failed", db.save() called

### Scenario 5: Payment gateway exception
**Setup**: Mock payment.charge() raises PaymentGatewayError
**Expected**: Exception propagates

### Scenario 6: Database save failure
**Setup**: Mock db.save() raises DatabaseError
**Expected**: Exception propagates, payment already charged

## Success Criteria
- [x] All scenarios covered
- [x] All dependencies properly mocked
- [x] Mock interactions verified
- [x] Error conditions tested
- [x] Coverage >90%
```

---

## Example 3: Async Function Test Plan

**Code**:
```
async def fetch_user_data(user_id, api_client):
    try:
        response = await api_client.get(f"/users/{user_id}")
        return response.json()
    except NetworkError as e:
        logger.error(f"Failed to fetch user {user_id}: {e}")
        raise UserFetchError(user_id) from e
```

**Test Plan**:
```markdown
# Test Plan: fetch_user_data

## Code Summary
- Purpose: Fetch user data from API asynchronously
- Type: Async Function with External Dependency
- Dependencies: API Client, Logger

## Test Strategy
- Unit Tests: 4 scenarios (async tests with mocked API)
- Coverage Target: 100%

## Mocking Strategy

**api_client.get()**:
- Approach: Mock with async return
- Returns: Mock response with .json() method
- Error scenarios: Raises NetworkError

**logger**:
- Approach: Mock
- Verify: error() called on failure

## Test Scenarios

### Scenario 1: Successful fetch
**Setup**: Mock api_client.get() returns successful response
**Expected**: Returns user data dict
**Assertions**: result == {"id": 123, "name": "Alice"}, mock_api.get.called_once_with("/users/123")

### Scenario 2: Network error
**Setup**: Mock api_client.get() raises NetworkError
**Expected**: Raises UserFetchError, logs error
**Assertions**: pytest.raises(UserFetchError, match="123"), mock_logger.error.called_once()

### Scenario 3: Edge case - user_id as string
**Input**: user_id="abc"
**Expected**: API called with "/users/abc"

### Scenario 4: Invalid JSON response
**Setup**: Mock response.json() raises JSONDecodeError
**Expected**: Exception propagates

## Python-Specific Notes
- Use pytest-asyncio for async test support
- Mark tests with @pytest.mark.asyncio
- Use async def for test functions
- Mock async functions with AsyncMock

## Success Criteria
- [x] All scenarios covered
- [x] Async properly tested
- [x] Error handling verified
- [x] Logging verified
```
