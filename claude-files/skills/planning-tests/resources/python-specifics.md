# Python-Specific Testing Details

## Testing Frameworks

**pytest** (Recommended):
- Modern, powerful, widely adopted (70%+ of Python projects)
- Fixtures for reusable setup
- Parametrization for multiple scenarios
- Rich assertion introspection
- Extensive plugin ecosystem

**unittest** (Standard Library):
- Built-in, no installation needed
- Class-based tests
- setUp/tearDown methods
- More verbose than pytest

## Essential pytest Patterns

### Fixtures
```python
@pytest.fixture
def sample_data():
    return {"id": 1, "name": "Alice"}

def test_process_data(sample_data):
    result = process(sample_data)
    assert result["processed"] is True
```

### Parametrization
```python
@pytest.mark.parametrize("input,expected", [
    (0, 0),
    (10, 10),
    (100, 100),
])
def test_calculate(input, expected):
    assert calculate(input) == expected
```

### Mocking with pytest-mock
```python
def test_api_call(mocker):
    mock_api = mocker.patch('mymodule.api_client')
    mock_api.get.return_value = {"status": "ok"}

    result = my_function()

    assert result == "ok"
    mock_api.get.assert_called_once()
```

### Async Testing
```python
@pytest.mark.asyncio
async def test_async_function():
    result = await async_function()
    assert result is not None
```

### Exception Testing
```python
def test_invalid_input_raises():
    with pytest.raises(ValueError, match="Invalid input"):
        process_data(invalid_input)
```

## Mocking Strategies

### When to Use Which

**unittest.mock.patch**:
```python
from unittest.mock import patch

@patch('mymodule.external_api')
def test_with_patch(mock_api):
    mock_api.get.return_value = "data"
    result = my_function()
    assert result == "data"
```

**pytest-mock (mocker fixture)**:
```python
def test_with_mocker(mocker):
    mock_api = mocker.patch('mymodule.external_api')
    mock_api.get.return_value = "data"
    result = my_function()
    assert result == "data"
```

**Key Rule**: **Patch where used, not where defined**
```python
# module_a.py
def api_call():
    return "real"

# module_b.py
from module_a import api_call

def my_function():
    return api_call()

# test_module_b.py
def test_my_function(mocker):
    # ✅ Correct: Patch where it's used (module_b)
    mocker.patch('module_b.api_call', return_value="mocked")

    # ❌ Wrong: Patch where it's defined (module_a)
    # mocker.patch('module_a.api_call', return_value="mocked")
```

### autospec for Safety
```python
def test_with_autospec(mocker):
    # Catches method name typos, signature mismatches
    mock_api = mocker.patch('mymodule.APIClient', autospec=True)
    mock_api.non_existent_method()  # Raises AttributeError
```

### Mocking Time
```python
# Using freezegun
from freezegun import freeze_time

@freeze_time("2024-01-15 10:00:00")
def test_time_dependent():
    result = get_current_time()
    assert result.hour == 10
```

### Mocking Random
```python
def test_random_function(mocker):
    mocker.patch('random.randint', return_value=5)
    result = function_using_random()
    assert result == 5
```

### Mocking Files
```python
def test_file_read(mocker):
    mock_open = mocker.patch('builtins.open',
                              mocker.mock_open(read_data='test data'))
    result = read_file('test.txt')
    assert result == 'test data'

# Or use tmp_path fixture for real files
def test_file_operations(tmp_path):
    test_file = tmp_path / "test.txt"
    test_file.write_text("content")
    result = process_file(test_file)
    assert result == "processed"
```

## Coverage

**Run with coverage**:
```bash
pytest --cov=src --cov-report=html --cov-report=term
```

**Coverage targets**:
- Library code: 70-90%
- Business logic: >90%
- Scripts/entry points: Excluded
- Type stubs: Excluded

## Test Organization

**Directory structure**:
```
project/
├── src/
│   └── mymodule/
│       ├── __init__.py
│       └── calculator.py
└── tests/
    └── mymodule/
        ├── __init__.py
        └── test_calculator.py
```

**Test file naming**: `test_<module>.py`

**Test function naming**: `test_<function>_<scenario>_<expected>`

## conftest.py

**Shared fixtures**:
```python
# tests/conftest.py
import pytest

@pytest.fixture
def db_connection():
    conn = create_connection()
    yield conn
    conn.close()

@pytest.fixture(scope="session")
def api_client():
    client = APIClient()
    yield client
    client.cleanup()
```

## Common Patterns

### Testing Classes
```python
class TestCalculator:
    @pytest.fixture
    def calculator(self):
        return Calculator()

    def test_add(self, calculator):
        assert calculator.add(2, 3) == 5

    def test_subtract(self, calculator):
        assert calculator.subtract(5, 3) == 2
```

### Testing Exceptions with Context
```python
def test_division_by_zero():
    calc = Calculator()
    with pytest.raises(ZeroDivisionError):
        calc.divide(10, 0)
```

### Testing Warnings
```python
def test_deprecated_function():
    with pytest.warns(DeprecationWarning):
        deprecated_function()
```

### Testing Logging
```python
def test_logging(caplog):
    my_function()
    assert "Expected log message" in caplog.text
```

### Markers for Test Organization
```python
@pytest.mark.slow
def test_slow_operation():
    # Long-running test
    pass

@pytest.mark.integration
def test_database_integration():
    # Integration test
    pass

# Run with: pytest -m "not slow"
```

## Property-Based Testing (Hypothesis)

For complex logic, use hypothesis:
```python
from hypothesis import given
import hypothesis.strategies as st

@given(st.integers(min_value=0, max_value=1000))
def test_always_positive(value):
    result = my_function(value)
    assert result >= 0
```

## Best Practices Summary

1. **Use pytest** - Modern, powerful, widely adopted
2. **Fixtures over setup/teardown** - More flexible, reusable
3. **Parametrize** - Test multiple scenarios efficiently
4. **Mock external deps only** - Use real objects for internal code
5. **Use autospec** - Catch interface mismatches
6. **Patch at import location** - Not where defined
7. **Test exceptions** - Use pytest.raises()
8. **Organize with markers** - Separate slow/fast, unit/integration
9. **Target 70-90% coverage** - Focus on quality, not 100%
10. **Use hypothesis for complex logic** - Property-based testing
