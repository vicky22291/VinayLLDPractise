# Skill Creation Guidelines

Comprehensive reference for building Claude Code skills. Extracted from Anthropic's official guide.

---

## 1. What is a Skill?

A skill is a folder that teaches Claude how to handle specific tasks or workflows. Instead of re-explaining preferences, processes, and domain expertise every conversation, skills let you teach Claude once.

### Folder Structure

```
your-skill-name/
├── SKILL.md            # Required - main instructions
├── scripts/            # Optional - executable code (Python, Bash)
├── references/         # Optional - documentation loaded as needed
└── assets/             # Optional - templates, fonts, icons
```

### Core Design Principles

| Principle | Description |
|-----------|-------------|
| **Progressive Disclosure** | 3-level system: frontmatter (always loaded) → SKILL.md body (when relevant) → linked files (on-demand) |
| **Composability** | Skills work alongside others - don't assume exclusive capability |
| **Portability** | Works across Claude.ai, Claude Code, and API without modification |
| **Token Efficiency** | ~30-50 tokens per skill until loaded; compact skills load faster |

---

## 2. Skill Categories

### Category 1: Document & Asset Creation
Creating consistent, high-quality output (documents, code, designs).
- Embedded style guides and brand standards
- Template structures for consistent output
- Quality checklists before finalizing
- No external tools required

### Category 2: Workflow Automation
Multi-step processes with consistent methodology.
- Step-by-step workflow with validation gates
- Templates for common structures
- Built-in review and improvement suggestions
- Iterative refinement loops

### Category 3: MCP Enhancement
Workflow guidance on top of MCP tool access.
- Coordinates multiple MCP calls in sequence
- Embeds domain expertise
- Provides context users would otherwise need to specify
- Error handling for common MCP issues

---

## 3. Technical Requirements

### Critical Rules

| Rule | Details |
|------|---------|
| **File name** | Must be exactly `SKILL.md` (case-sensitive). No variations. |
| **Folder name** | kebab-case only. No spaces, underscores, or capitals. |
| **No README.md** | Don't include README.md inside skill folder. All docs go in SKILL.md or references/. |
| **No XML in frontmatter** | Angle brackets (`< >`) are forbidden in YAML frontmatter. |
| **Reserved names** | Cannot use "claude" or "anthropic" in skill name. |

### YAML Frontmatter

**Minimal required format:**

```yaml
---
name: your-skill-name
description: What it does. Use when user asks to [specific phrases].
---
```

### Field Reference

| Field | Required | Rules |
|-------|----------|-------|
| `name` | Yes | kebab-case, no spaces/capitals, should match folder name |
| `description` | Yes | Must include WHAT + WHEN, under 1024 chars, no XML tags |
| `license` | No | e.g., MIT, Apache-2.0 |
| `allowed-tools` | No | Restrict tool access: `"Read, Write(output/**), Bash(pytest *)"` |
| `compatibility` | No | 1-500 chars, environment requirements |
| `metadata` | No | Custom key-value pairs (author, version, mcp-server, etc.) |

### Full Frontmatter Example

```yaml
---
name: sprint-planner
description: Manages Linear project workflows including sprint planning, task creation, and status tracking. Use when user mentions "sprint", "Linear tasks", "project planning", or asks to "create tickets".
license: MIT
allowed-tools: "Read, Write(.claude/skills/**), Bash(mkdir *)"
metadata:
  author: Team Name
  version: 1.0.0
  mcp-server: linear
  category: productivity
  tags: [project-management, automation]
---
```

---

## 4. Writing Effective Descriptions

The `description` field is the most important part - it determines when Claude loads your skill.

**Formula:** `[What it does] + [When to use it] + [Key capabilities]`

### Good Descriptions

```yaml
# Specific and actionable
description: Analyzes Figma design files and generates developer handoff documentation. Use when user uploads .fig files, asks for "design specs", "component documentation", or "design-to-code handoff".

# Includes trigger phrases
description: Manages Linear project workflows including sprint planning, task creation, and status tracking. Use when user mentions "sprint", "Linear tasks", "project planning", or asks to "create tickets".

# Clear value proposition
description: End-to-end customer onboarding workflow for PayFlow. Handles account creation, payment setup, and subscription management. Use when user says "onboard new customer", "set up subscription", or "create PayFlow account".
```

### Bad Descriptions

```yaml
# Too vague
description: Helps with projects.

# Missing triggers
description: Creates sophisticated multi-page documentation systems.

# Too technical, no user triggers
description: Implements the Project entity model with hierarchical relationships.
```

### Debugging Triggers

Ask Claude: *"When would you use the [skill name] skill?"* - Claude will quote the description back. Adjust based on what's missing.

### Preventing Over-triggering

```yaml
# Add negative triggers
description: Advanced data analysis for CSV files. Use for statistical modeling, regression, clustering. Do NOT use for simple data exploration (use data-viz skill instead).

# Clarify scope
description: PayFlow payment processing for e-commerce. Use specifically for online payment workflows, not for general financial queries.
```

---

## 5. Writing Instructions

### Recommended SKILL.md Structure

```markdown
---
name: your-skill
description: [...]
---

# Your Skill Name

## Instructions

### Step 1: [First Major Step]
Clear explanation of what happens.
[Tool calls, validations, expected output]

### Step 2: [Second Major Step]
...

## Examples

### Example 1: [Common scenario]
User says: "..."
Actions: [numbered list]
Result: [expected outcome]

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| [Common error] | [Why] | [How to fix] |
```

### Best Practices for Instructions

**Be specific and actionable:**

```markdown
# Good
Run `python scripts/validate.py --input {filename}` to check data format.
If validation fails, common issues include:
- Missing required fields (add them to the CSV)
- Invalid date formats (use YYYY-MM-DD)

# Bad
Validate the data before proceeding.
```

**Reference bundled resources clearly:**

```markdown
Before writing queries, consult `references/api-patterns.md` for:
- Rate limiting guidance
- Pagination patterns
- Error codes and handling
```

**Use progressive disclosure:**
- Keep SKILL.md focused on core instructions
- Move detailed documentation to `references/`
- Link to references instead of inlining

**Include error handling:**

```markdown
## Common Issues

### MCP Connection Failed
If you see "Connection refused":
1. Verify MCP server is running
2. Confirm API key is valid
3. Try reconnecting
```

**For critical validations, use scripts:**
Code is deterministic; language interpretation isn't. Bundle scripts for checks that must be reliable.

**Combat model laziness:**

```markdown
## Performance Notes
- Take your time to do this thoroughly
- Quality is more important than speed
- Do not skip validation steps
```

> Note: Adding this to user prompts is more effective than in SKILL.md.

---

## 6. Size Guidelines

| Skill Type | Target Lines | Strategy |
|------------|-------------|----------|
| Simple/typical | < 200 lines | Single SKILL.md |
| Complex | < 300 lines | SKILL.md + REFERENCE.md |
| Very complex | < 500 lines SKILL.md | Split to references/, use progressive disclosure |

**Keep compact:**
- Use tables instead of long descriptions
- Condense step descriptions to 1-2 lines max
- Remove redundant explanations
- Use bullet lists instead of paragraphs

---

## 7. Workflow Patterns

### Pattern 1: Sequential Workflow Orchestration
Use when users need multi-step processes in a specific order.

```markdown
### Step 1: Create Account
Call MCP tool: `create_customer`
Parameters: name, email, company

### Step 2: Setup Payment
Call MCP tool: `setup_payment_method`
Wait for: payment method verification

### Step 3: Create Subscription
Call MCP tool: `create_subscription`
Parameters: plan_id, customer_id (from Step 1)
```

**Key techniques:** Explicit step ordering, dependencies between steps, validation at each stage, rollback instructions.

### Pattern 2: Multi-MCP Coordination
Use when workflows span multiple services.

**Key techniques:** Clear phase separation, data passing between MCPs, validation before next phase, centralized error handling.

### Pattern 3: Iterative Refinement
Use when output quality improves with iteration.

```markdown
### Initial Draft
1. Fetch data via MCP
2. Generate first draft
3. Save to temporary file

### Quality Check
1. Run validation script
2. Identify issues

### Refinement Loop
1. Address each issue
2. Regenerate affected sections
3. Re-validate
4. Repeat until quality threshold met
```

**Key techniques:** Explicit quality criteria, validation scripts, know when to stop iterating.

### Pattern 4: Context-Aware Tool Selection
Use when same outcome needs different tools depending on context.

**Key techniques:** Clear decision criteria, fallback options, transparency about choices.

### Pattern 5: Domain-Specific Intelligence
Use when skill adds specialized knowledge beyond tool access.

**Key techniques:** Domain expertise embedded in logic, compliance-before-action, comprehensive documentation.

---

## 8. Testing

### Three Testing Areas

#### 1. Triggering Tests
Does it load at the right times?

```markdown
Should trigger:
- "Help me set up a new ProjectHub workspace"
- "I need to create a project in ProjectHub"
- "Initialize a ProjectHub project for Q4 planning"

Should NOT trigger:
- "What's the weather in San Francisco?"
- "Help me write Python code"
```

#### 2. Functional Tests
Does it produce correct outputs?

```markdown
Test: Create project with 5 tasks
Given: Project name "Q4 Planning", 5 task descriptions
When: Skill executes workflow
Then:
- Project created
- 5 tasks created with correct properties
- No API errors
```

#### 3. Performance Comparison
Does the skill improve results vs. baseline?

```markdown
Without skill:
- 15 back-and-forth messages
- 3 failed API calls
- 12,000 tokens consumed

With skill:
- 2 clarifying questions
- 0 failed API calls
- 6,000 tokens consumed
```

### Success Criteria (Aspirational Targets)

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Trigger accuracy | 90% on relevant queries | Run 10-20 test queries |
| Workflow efficiency | Fewer tool calls | Compare with/without skill |
| API reliability | 0 failed calls per workflow | Monitor logs during tests |
| User corrections | None needed | Run same request 3-5 times |

### Iteration Signals

| Signal | Type | Solution |
|--------|------|----------|
| Skill doesn't load when it should | Under-triggering | Add more trigger phrases to description |
| Users manually enabling it | Under-triggering | Add keywords, especially technical terms |
| Skill loads for unrelated queries | Over-triggering | Add negative triggers, be more specific |
| Inconsistent results | Execution | Improve instructions, add error handling |
| API call failures | Execution | Add retry logic, validate inputs |

---

## 9. Distribution

### For Claude Code
Place in `.claude/skills/{skill-name}/` directory.

### For Claude.ai
1. Zip the skill folder
2. Upload via Settings > Capabilities > Skills

### For Organizations
Admins can deploy skills workspace-wide with automatic updates.

### For API
- `/v1/skills` endpoint for listing and managing skills
- Add skills via `container.skills` parameter in Messages API
- Requires Code Execution Tool beta

### GitHub Distribution
- Host on GitHub with public repo
- Clear README (repo-level, NOT inside skill folder)
- Example usage with screenshots
- Link from MCP documentation if applicable

---

## 10. Quick Validation Checklist

### Before Upload
- [ ] Folder named in kebab-case
- [ ] `SKILL.md` exists (exact spelling, case-sensitive)
- [ ] YAML frontmatter has `---` delimiters
- [ ] `name` field: kebab-case, no spaces, no capitals
- [ ] `description` includes WHAT and WHEN with trigger phrases
- [ ] No XML tags (`< >`) in frontmatter
- [ ] Instructions are clear and actionable
- [ ] Error handling included
- [ ] Examples provided (3-5)
- [ ] References clearly linked
- [ ] SKILL.md is compact (< 200 typical, < 300 complex)

### After Upload
- [ ] Triggers on obvious tasks
- [ ] Triggers on paraphrased requests
- [ ] Does NOT trigger on unrelated topics
- [ ] Functional tests pass
- [ ] Tool integration works

### Ongoing
- [ ] Monitor for under/over-triggering
- [ ] Collect user feedback
- [ ] Iterate on description and instructions
- [ ] Update version in metadata

---

## 11. Common Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| "Could not find SKILL.md" | File not named exactly `SKILL.md` | Rename (case-sensitive) |
| "Invalid frontmatter" | YAML formatting issue | Check `---` delimiters, quote strings, validate syntax |
| "Invalid skill name" | Name has spaces or capitals | Use kebab-case only |
| Skill won't trigger | Description too vague | Add specific trigger phrases, include "Use when..." |
| Skill triggers too often | Description too broad | Add negative triggers, narrow scope |
| Instructions not followed | Too verbose or ambiguous | Condense, use bullets, put critical items first |
| Slow/degraded responses | Too much content loaded | Move docs to references/, keep SKILL.md < 5000 words |
| Incomplete execution | SKILL.md too long (>500 lines) | Split to SKILL.md + REFERENCE.md |

---

*Source: "The Complete Guide to Building Skills for Claude" - Anthropic (January 2026)*
