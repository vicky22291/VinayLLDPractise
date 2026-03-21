---
name: planning-tests
description: Creates comprehensive test plans identifying test scenarios, edge cases, mocking strategy, and coverage approach. Use before writing any tests, during TDD, when user says "plan tests", "test this module", or "what should I test". MUST be invoked before writing test code.
allowed-tools: "Read"
metadata:
  version: 2.0.0
  category: testing
  author: AI Assistant
---

# Test Planning

**Purpose**: Generate comprehensive test plans before writing tests

**Use Cases**: Planning tests for new code, TDD planning phase, identifying edge cases, determining mocking strategy

**Output**: Structured test plan with scenarios, edge cases, mocking strategy, and success criteria

**Examples**: See [references/examples.md](references/examples.md) for 3 complete test plan examples (pure function, class with deps, async function)

---

## Instructions

<instructions>

### Phase 1: Code Analysis

1. **Read and understand the code**: Identify purpose, public methods, dependencies, inputs/outputs/side effects
2. **Classify the code type**:
   - **Pure Function**: No side effects, same input -> same output
   - **Stateful Class**: Maintains internal state
   - **Integration Point**: Interacts with external systems (DB, API, filesystem)
   - **Async/Concurrent**: Uses async/await or threading
   - **Complex Logic**: Multiple branches, loops, calculations

### Phase 2: Test Categorization

| Test Type | When | Required For |
|-----------|------|-------------|
| **Unit Tests** | Isolated, fast, mocked deps | All code |
| **Integration Tests** | Real dependencies | DB, API, file operations |
| **Property Tests** | Invariants, random input | Complex calculations, data transforms |

### Phase 3: Scenario Identification

For each public method/function, identify:

1. **Happy Path**: Valid inputs, expected behavior, typical workflows
2. **Edge Cases**: Boundary values (min/max/zero/empty), special values (None, infinity, NaN), large inputs
3. **Error Conditions**: Invalid inputs, precondition violations, external failures, wrong state
4. **State Transitions** (if stateful): Valid transitions, invalid transitions, persistence

### Phase 4: Mocking Strategy

For each external dependency:

| Decision | Mock | Don't Mock |
|----------|------|------------|
| **What** | DB, APIs, filesystem, network, time, random | Pure functions, value objects, simple data structures |
| **Approach** | **Stub** (fixed data), **Mock** (verify interactions), **Fake** (in-memory impl) | - |

Document: What it returns for valid/invalid inputs, side effects to verify.

### Phase 5: Test Structure Planning

For each scenario:
```
Test Name: test_<method>_<scenario>_<expected_outcome>
Setup (Arrange): Create data/fixtures, initialize mocks, set preconditions
Execution (Act): Call method, capture result
Verification (Assert): Check return value, verify state, verify mock calls
```

### Phase 6: Coverage Strategy

**Prioritize**:
1. **Critical**: Business logic, security, data integrity (must be 100%)
2. **High**: Error handling, edge cases, state management
3. **Medium**: Happy paths, common scenarios
4. **Low**: Simple getters/setters, trivial code

**Target**: 70-90% line coverage, higher for critical paths

### Phase 7: Generate Test Plan Document

Use the output format below. See [references/examples.md](references/examples.md) for complete examples.

</instructions>

---

## Guidelines

<guidelines>

### Do's
- Analyze code thoroughly before planning
- Identify all scenarios: happy path, edge cases, errors
- Plan mocking strategy: mock external deps, real objects for internal
- Use descriptive test names: `test_method_scenario_outcome`
- Document expected behavior with clear assertions
- Plan for edge cases: boundaries, empty, None, large values
- Set realistic coverage targets (70-90%)
- Prioritize critical code: business logic, security, data integrity

### Don'ts
- Don't plan tests for external code (mock it)
- Don't test implementation details (test behavior through public API)
- Don't over-mock (use real objects for simple collaborators)
- Don't skip edge cases (they catch the most bugs)
- Don't aim for 100% coverage blindly (quality > percentage)
- Don't test trivial code (simple getters/setters)
- Don't mix unit and integration (keep separate)

</guidelines>

---

## Validation Checklist

<validation>

- [ ] All public methods/functions have test scenarios
- [ ] Happy path, edge cases, error conditions covered
- [ ] State transitions covered (if stateful)
- [ ] All external dependencies identified with mock approach
- [ ] Test names are descriptive, AAA pattern followed
- [ ] Coverage target set realistically, critical paths identified
- [ ] Test plan is actionable (developer can implement directly)

</validation>

---

## Output Format

```markdown
# Test Plan: {Name}

## Code Summary
- Purpose: {What it does}
- Type: {Pure/Stateful/Integration/Async/Complex}
- Dependencies: {List}

## Test Strategy
- Unit Tests: {Count} scenarios
- Integration Tests: {Count} (if applicable)
- Coverage Target: {Percentage}%

## Mocking Strategy
{For each dependency: approach + behavior}

## Test Scenarios
### Scenario 1: {Name}
**Type**: Unit/Integration
**Setup**: {Required setup}
**Input**: {Test input}
**Expected**: {Expected outcome}
**Assertions**: {What to verify}

## Edge Cases
- {List}

## Error Conditions
- {List}

## Success Criteria
- [ ] All scenarios have tests
- [ ] Coverage target met
- [ ] All tests pass
```

---

## References

- **Examples**: [references/examples.md](references/examples.md) - 3 complete test plan examples
- **Python-specific**: [resources/python-specifics.md](resources/python-specifics.md) - pytest, fixtures, async testing

## Core Principles

**Test Behavior, Not Implementation** | **Isolation** | **Fast & Repeatable** | **One Assertion Per Concept** | **Readable Tests** | **F.I.R.S.T.** (Fast, Isolated, Repeatable, Self-validating, Timely)
