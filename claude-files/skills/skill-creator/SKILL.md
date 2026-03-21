---
name: skill-creator
description: Creates new Claude Code skills following best practices. Use when user wants to create a new skill, automate a workflow, or teach Claude a process.
allowed-tools: "Read, Write(.claude/skills/**), Bash(mkdir *)"
metadata:
  version: 2.0.0
  category: development
  author: AI Assistant
---

# Skill Creator

**Purpose**: Guide creation of new Claude Code skills following best practices

**References**:
- [resources/examples.md](resources/examples.md) - 3 complete creation examples with validation checkpoints
- [resources/skill-creation-guidelines.md](resources/skill-creation-guidelines.md) - Comprehensive guidelines from Anthropic's official guide

---

## Instructions

<instructions>

### Phase 1: Requirements Gathering

Ask these questions (one at a time):
1. **What should this skill do?** (Purpose and goals)
2. **When should it be invoked?** (Triggers and use cases)
3. **What inputs/outputs?** (Parameters, files, context -> files, reports, actions)
4. **What tools does it need?** (Read, Write, Bash, WebSearch, etc.)
5. **Does it need external scripts?** (Validation, processing)
6. **What are 3-5 example scenarios?** (For few-shot learning)

### Phase 2: Skill Design

1. **Name**: kebab-case, gerund form (verb+-ing), max 64 chars (e.g., `generating-reports`)
2. **Description** (max 1024 chars): `"[What it does] [When to use it]"` - include trigger phrases
3. **Structure**: Single SKILL.md if <200 lines, + REFERENCE.md if complex, + resources/ for supporting files
4. **allowed-tools**: Only what's needed, use wildcards: `"Read, Write(output/**), Bash(pytest *)"`

### Phase 3: Create Skill Files

```bash
mkdir -p .claude/skills/{skill-name}/resources
```

**Generate SKILL.md** - CRITICAL: compact and concise:
- Use tables instead of long descriptions
- Condense steps to 1-2 lines max
- Target: <200 lines typical, <300 complex
- Use the structure below

```yaml
---
name: {skill-name}
description: {description}
allowed-tools: "{tools}"
metadata:
  version: 1.0.0
  category: {category}
---

# {Skill Name}

**When to Invoke**: {Specific triggers}
**Purpose**: {One-sentence purpose}

---

## Workflow (N-Step Process)

### Step 1: {Name}
{1-2 line description}

[Continue steps - keep minimal]

---

## Examples
### Example 1: {Title}
{Condensed: input -> action -> output}

---

## Key Rules
**Do's**: {Bullet list}
**Don'ts**: {Bullet list}

---

## Common Issues
| Issue | Cause | Fix |
|-------|-------|-----|
```

### Phase 4: Validation

**Structure checks**:
- [ ] SKILL.md exists with valid YAML frontmatter
- [ ] Name: kebab-case, max 64 chars
- [ ] Description: what + when, max 1024 chars, no XML tags (`< >`)
- [ ] Compact: <200 lines typical, <300 complex
- [ ] 3-5 examples included
- [ ] Error handling specified

**Best practice checks**:
- [ ] Single-purpose (not "everything")
- [ ] Explicit error handling
- [ ] Validation checkpoints at critical steps
- [ ] Examples show input -> process -> output

### Phase 5: Testing

1. Use explicit prompt: "Use the {skill-name} skill to..."
2. Verify it loads and executes correctly
3. If not invoked: improve description. If incomplete: add checkpoints. If unexpected: add constraints.

### Phase 6: Delivery

Report to user:
```
Skill created: {skill-name}
Location: .claude/skills/{skill-name}/
Description: {description}
Tools: {allowed-tools}

Try: "Use the {skill-name} skill to {example scenario}"

Next: Test -> Iterate -> git add .claude/skills/{skill-name}/
```

</instructions>

---

## Key Rules

**Do's**:
- Ask questions one at a time
- Document requirements before designing
- Create compact skills (tables/bullets, 1-2 line descriptions)
- Include 3-5 concrete examples
- Add explicit error handling
- Enforce workflow discipline (save state BEFORE changes)
- Target <200 lines typical, <300 complex

**Don'ts**:
- Don't create "everything" skills (keep single-purpose)
- Don't be verbose (no repetition, long paragraphs)
- Don't use vague descriptions (include what + when + triggers)
- Don't skip examples (critical for Claude's understanding)
- Don't nest references deep (one level only)
- Don't hard-code values (parameterize)

---

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Skill not invoked | Description doesn't match use case | Add trigger phrases, "Use when..." |
| Incomplete execution | SKILL.md too long (>500 lines) | Split to SKILL.md + REFERENCE.md |
| Unexpected behavior | Ambiguous instructions | Add constraints, more examples |
| Skill not loading | Invalid YAML frontmatter | Validate syntax, check `---` delimiters |

---

## Core Principles

- **Compactness First**: Tables, bullets, 1-2 line descriptions
- **Progressive Disclosure**: Metadata (always) -> Instructions (when relevant) -> Resources (on-demand)
- **Token Efficiency**: ~30-50 tokens per skill until loaded
- **Description is Critical**: Determines when Claude invokes your skill
- **Examples Drive Behavior**: 3-5 examples dramatically improve execution quality
