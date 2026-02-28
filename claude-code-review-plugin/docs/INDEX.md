# Claude Code Review Plugin - Documentation Index

IntelliJ IDEA plugin + standalone CLI that provides a **Git-review-like experience** for reviewing Markdown documents and Git diffs with bidirectional Claude Code integration. The plugin overlays inline commenting on existing editors (gutter icons, comment popups, line highlighting), publishes all comments to a structured `.review.json` file, and Claude responds via a standalone CLI tool.

---

## Documentation Map

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layered architecture, class inventory, service interactions, logic vs platform split |
| [DATA_MODEL.md](DATA_MODEL.md) | Model classes, state machines, JSON schema, review file naming conventions |
| [REVIEW_FLOW.md](REVIEW_FLOW.md) | End-to-end review lifecycle: 7 phases from start to archive |
| [INTELLIJ_PLATFORM.md](INTELLIJ_PLATFORM.md) | Plugin configuration, extension points, IntelliJ APIs, visual overlays, colors |
| [CLI.md](CLI.md) | review-cli commands, JSON format, `/review-respond` skill reference |
| [TESTING.md](TESTING.md) | Test inventory, dual constructors, coverage configuration, testing patterns |
| [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) | Conventions, sealed dispatch, observer pattern, adding new review types |

## Quick Stats

| Metric | Value |
|--------|-------|
| Source files | 45 Kotlin files |
| Test files | 16 Kotlin files |
| Test methods | 485 `@Test` methods |
| Coverage minimum | 95% (JaCoCo) |
| SVG icons | 5 |
| IDE actions | 10 registered |
| IDE extensions | 7 registered |
| IDE services | 3 project-level |

## Related Resources

- **CLAUDE.md** (project root) -- Build commands, tech stack, keyboard shortcuts
- **Design docs**: `../projects/review-plugin/` -- PRD, HLD, implementation plan, task breakdown
- **CLI source**: `review-cli/` -- Standalone CLI subproject
- **Skill**: `.claude/commands/review-respond.md` -- Claude Code slash command
