# Skill Creator Examples

## Example 1: Creating Test Generator Skill

**User Request**: "Create a skill that generates unit tests"

**Requirements Gathering**:
- Purpose: Generate pytest unit tests with mocking
- When: User asks to test a function/class
- Inputs: Source code file path
- Outputs: Test file in tests/ directory
- Tools: Read, Write(tests/**), Bash(pytest *)
- Scripts: Yes - coverage checker
- Examples: Test pure function, test class with deps, test async

**Design**:
- Name: `generating-unit-tests`
- Description: "Generates pytest unit tests with proper mocking and >90% coverage. Use when asked to test Python code, create tests, or implement TDD."
- Structure: SKILL.md + scripts/check_coverage.py
- Tools: `"Read, Write(tests/**), Bash(pytest *), Bash(coverage *)"`

**Created Files**:
```
.claude/skills/generating-unit-tests/
├── SKILL.md (450 lines)
├── scripts/
│   └── check_coverage.py
└── resources/
    └── examples.md
```

**Testing**:
- "Use the generating-unit-tests skill to test calculator.py"
- Generated test_calculator.py with fixtures, mocks, parametrization
- All tests pass, coverage 95%

---

## Example 2: Creating Documentation Skill

**User Request**: "Automate API documentation generation"

**Requirements Gathering**:
- Purpose: Generate API docs from code
- When: User asks to document API, create docs
- Inputs: Source code directory
- Outputs: Markdown docs in docs/api/
- Tools: Read, Write(docs/**), Bash(grep *)
- Scripts: Yes - doc validator
- Examples: REST API, GraphQL, WebSocket

**Design**:
- Name: `generating-api-docs`
- Description: "Generates comprehensive API documentation from source code. Use when documenting APIs, creating developer guides, or updating API references."
- Structure: SKILL.md + REFERENCE.md (for templates)
- Tools: `"Read, Write(docs/**), Grep(*.py)"`

**Created Files**:
```
.claude/skills/generating-api-docs/
├── SKILL.md (400 lines)
├── REFERENCE.md (600 lines - templates)
└── resources/
    └── doc-template.md
```

**Testing**:
- "Use the generating-api-docs skill to document src/api/"
- Generated docs/api/endpoints.md, docs/api/authentication.md
- Proper formatting, all endpoints documented

---

## Example 3: Creating Code Review Skill

**User Request**: "Review code for best practices"

**Requirements Gathering**:
- Purpose: Review code for issues, suggest improvements
- When: Before commits, during PR reviews
- Inputs: File paths or git diff
- Outputs: Review report with issues + suggestions
- Tools: Read, Bash(git *), Bash(flake8), Bash(mypy)
- Scripts: No - uses existing linters
- Examples: Review module, review PR, review refactoring

**Design**:
- Name: `reviewing-code`
- Description: "Reviews Python code for best practices, style issues, and potential bugs. Use when reviewing code, checking PRs, or validating changes."
- Structure: SKILL.md only (under 500 lines)
- Tools: `"Read, Bash(git *), Bash(flake8 *), Bash(mypy *)"`

**Created Files**:
```
.claude/skills/reviewing-code/
└── SKILL.md (480 lines)
```

**Testing**:
- "Use the reviewing-code skill to review src/calculator.py"
- Found 3 style issues, 1 type error, 2 improvement suggestions
- Provided specific fixes with line numbers

---

## Validation Checkpoints

After each phase, verify:

**Phase 1 Complete**:
- [ ] All 7 questions answered
- [ ] Requirements documented clearly
- [ ] User confirmed understanding

**Phase 2 Complete**:
- [ ] Skill name is valid (kebab-case, gerund, <64 chars)
- [ ] Description is clear (what + when, <1024 chars)
- [ ] Structure planned (SKILL.md +/- REFERENCE.md +/- resources/scripts/)
- [ ] Tools identified and scoped appropriately

**Phase 3 Complete**:
- [ ] Directory structure created
- [ ] SKILL.md generated with all sections
- [ ] REFERENCE.md created if needed
- [ ] Scripts created if needed
- [ ] All files use proper format

**Phase 4 Complete**:
- [ ] Structure validation passed (all checkboxes)
- [ ] Best practices review passed
- [ ] Test readiness confirmed

**Phase 5 Complete**:
- [ ] Test scenarios created (3+ scenarios)
- [ ] Invocation tested successfully
- [ ] Outputs verified
- [ ] Improvements documented

**Phase 6 Complete**:
- [ ] Documentation created
- [ ] User report delivered
- [ ] Next steps provided

If any checkpoint fails, address issues before proceeding.
