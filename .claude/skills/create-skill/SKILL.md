---
name: create-skill
description: Creates new Claude Code skills with proper structure, frontmatter, and instructions. Use when user asks to "create a skill", "make a skill", "new skill", "build a skill", or "add a skill".
metadata:
  author: Risk Platform
  version: 1.1.0
  category: workflow-automation
  tags: [skills, automation, meta]
---

# Skill Creator

Create well-structured Claude Code skills following Anthropic's official guidelines.

For detailed reference on all rules, patterns, and testing: see [references/skill-creation-guidelines.md](references/skill-creation-guidelines.md)

## Instructions

### Phase 1: Gather Requirements

Ask the user (use AskUserQuestion or infer from context):

1. **What does the skill do?** — Core purpose in one sentence
2. **When should it trigger?** — Specific phrases/scenarios that activate it
3. **What does it produce?** — Output format, files created, actions taken
4. **Show me 2-3 example usages** — Concrete input -> output examples

Infer tools, scripts, and inputs from the answers above — don't ask separately.

### Phase 2: Design the Skill

1. **Choose the name** (max 64 characters):
   - **Prefer gerund form**: `processing-pdfs`, `reviewing-documents`, `testing-code`
   - Acceptable alternatives: `pdf-processing`, `process-pdfs`
   - kebab-case only, cannot contain "claude" or "anthropic"

2. **Write the description** (most important field):
   - Formula: `[What it does] + [When to use it]`
   - **Must be third person** (e.g., "Processes files..." not "I process files..." or "You can use this to...")
   - Must include trigger phrases: "Use when user asks to..."
   - Under 1024 characters, no XML tags (`< >`)

3. **Set degrees of freedom** for instructions:

   | Task Fragility | Freedom | Instruction Style |
   |---------------|---------|-------------------|
   | Fragile (migrations, APIs) | Low | Exact scripts, no deviation |
   | Standard (workflows) | Medium | Pseudocode with parameters |
   | Flexible (reviews, analysis) | High | Text-based heuristics |

4. **Plan the structure**:

   | Skill Complexity | Structure |
   |-----------------|-----------|
   | Simple (< 200 lines) | `SKILL.md` only |
   | Medium (< 300 lines) | `SKILL.md` + `references/` |
   | Complex (< 500 lines) | `SKILL.md` + `references/` + `scripts/` |

### Phase 3: Create the Skill

5. **Create the directory**:
   ```
   .claude/skills/{skill-name}/
   ├── SKILL.md            # Required
   ├── references/         # Optional - linked from SKILL.md, loaded on demand
   └── scripts/            # Optional - executable code
   ```

6. **Write SKILL.md** with this structure:

   ```markdown
   ---
   name: {skill-name}
   description: {third-person description with trigger phrases}
   metadata:
     author: {team}
     version: 1.0.0
     category: {category}
     tags: [{tags}]
   ---

   # {Skill Title}

   {One-line summary}

   For detailed reference: see [references/details.md](references/details.md)

   ## Instructions

   ### Step 1: {First Step}
   {Clear, actionable instructions}

   ### Step 2: {Second Step}
   ...

   ## Examples

   ### Example 1: {Scenario}
   **Input**: {what the user says}
   **Output**: {what gets produced}

   ## Troubleshooting

   | Issue | Fix |
   |-------|-----|
   | {problem} | {solution} |
   ```

7. **Write reference files** if needed:
   - Move detailed docs, templates, and examples to `references/`
   - **Link explicitly** from SKILL.md: "See [references/patterns.md](references/patterns.md) for..."
   - Keep references **one level deep** (no reference linking to another reference)
   - Add a **table of contents** to any reference file over 100 lines

8. **Write scripts** if needed:
   - For deterministic checks that language interpretation can't handle
   - Handle errors in scripts — don't punt to Claude

### Phase 4: Validate

9. **Run through this checklist**:

   ```
   Validation Progress:
   - [ ] Folder is kebab-case
   - [ ] SKILL.md exists (exact name, case-sensitive)
   - [ ] No README.md inside skill folder
   - [ ] name is kebab-case, matches folder, under 64 chars
   - [ ] description is third person, has WHAT + WHEN, under 1024 chars
   - [ ] No XML tags in frontmatter
   - [ ] Instructions are specific and actionable
   - [ ] 2-3 examples provided
   - [ ] Troubleshooting section included
   - [ ] SKILL.md under 500 lines
   - [ ] References linked explicitly (not inlined)
   - [ ] Reference files have TOC if over 100 lines
   ```

10. **If validation fails**: fix the issues and re-validate. Repeat until all checks pass.

11. **Report to user**:
    - Skill location
    - How to invoke it
    - Trigger phrases that will activate it
    - Suggest testing with: "Ask Claude: 'When would you use the {skill-name} skill?'"

---

## Examples

### Example 1: Simple Skill (single file)

**User**: "Create a skill that generates commit messages"

**Design**:
- Name: `generating-commits` (gerund form)
- Description: "Generates descriptive commit messages by analyzing git diffs. Use when the user asks to 'write a commit message', 'generate commit', or 'summarize changes'."
- Structure: SKILL.md only

**Output**:
```
.claude/skills/generating-commits/
└── SKILL.md (100 lines)
```

### Example 2: Medium Skill (with references)

**User**: "Create a skill for analyzing CSV data"

**Design**:
- Name: `analyzing-data` (gerund form)
- Description: "Analyzes CSV and Excel files to produce statistical summaries, pivot tables, and charts. Use when user asks to 'analyze data', 'summarize spreadsheet', or 'generate report from CSV'."
- Structure: SKILL.md + references/

**Output**:
```
.claude/skills/analyzing-data/
├── SKILL.md (200 lines)
└── references/
    └── chart-patterns.md
```

### Example 3: Complex Skill (with scripts)

**User**: "Create a skill that validates API schemas"

**Design**:
- Name: `validating-schemas` (gerund form)
- Description: "Validates OpenAPI and JSON Schema files against style guides and detects breaking changes. Use when user asks to 'validate schema', 'check API spec', or 'detect breaking changes'."
- Structure: SKILL.md + references/ + scripts/

**Output**:
```
.claude/skills/validating-schemas/
├── SKILL.md (250 lines)
├── references/
│   └── style-rules.md
└── scripts/
    └── validate_schema.py
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "Could not find SKILL.md" | File must be exactly `SKILL.md` (case-sensitive) |
| "Invalid frontmatter" | Check `---` delimiters, no XML tags, valid YAML |
| "Invalid skill name" | kebab-case only, max 64 chars, no reserved words |
| Skill doesn't trigger | Add more trigger phrases to description |
| Skill triggers too often | Add scope limits: "Do NOT use for..." |
| Instructions not followed | Condense, use bullets, put critical items first |
| Slow responses | Move docs to `references/`, keep SKILL.md compact |
