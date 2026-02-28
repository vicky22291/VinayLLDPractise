# Claude Code Review Plugin

IntelliJ IDEA plugin + CLI that provides a **Git-review-like experience** for reviewing Markdown documents and Git diffs with bidirectional Claude Code integration. The plugin overlays inline commenting on existing editors (gutter icons, comment popups, line highlighting), publishes all comments to a structured `.review.json` file, and Claude responds via a standalone CLI tool — creating a batch review loop without terminal context-switching.

**Design docs**: `../projects/review-plugin/` (PRD, HLD, IMPLEMENTATION, task breakdown)

**Investigation order**: When investigating bugs, understanding behavior, or planning changes — **always read the `docs/` first** (start with `docs/INDEX.md`, then the relevant doc for the area). Only go to source code if the docs don't answer the question or you need to verify implementation details.

**Key design principles:**
1. **Overlay, not replace** — adds a comment layer on top of existing Markdown editor and Git4Idea diff viewer
2. **Opt-in only** — review mode is explicitly activated; no interference with normal editing
3. **JSON-based integration** — plugin and CLI communicate through `.review.json` files; no direct API calls
4. **Bidirectional** — user writes comments, Claude writes responses, user can reply — all in the same file

---

## Detailed Documentation

See [`docs/INDEX.md`](docs/INDEX.md) for the full documentation index. Key docs:

| Document | Covers |
|----------|--------|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Layers, class inventory, service interactions, logic vs platform split |
| [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) | Model classes, state machines, JSON schema, naming conventions |
| [`docs/REVIEW_FLOW.md`](docs/REVIEW_FLOW.md) | End-to-end lifecycle with sequence diagrams (7 phases) |
| [`docs/INTELLIJ_PLATFORM.md`](docs/INTELLIJ_PLATFORM.md) | plugin.xml, extension points, IntelliJ APIs, visual overlays, colors |
| [`docs/CLI.md`](docs/CLI.md) | review-cli commands, JSON format, `/review-respond` skill |
| [`docs/TESTING.md`](docs/TESTING.md) | Test inventory, dual constructors, coverage config, patterns |
| [`docs/DEVELOPMENT_GUIDE.md`](docs/DEVELOPMENT_GUIDE.md) | Conventions, sealed dispatch, adding new review types |

---

## Project Structure

```
claude-code-review-plugin/
├── src/main/kotlin/com/uber/jetbrains/reviewplugin/
│   ├── model/          # Data classes: ReviewSession (sealed), ReviewComment, enums
│   ├── services/       # Core logic: ReviewModeService, CommentService, StorageManager, ReviewFileManager, GitDiffService
│   ├── actions/        # IDE actions: StartMarkdownReview, StartDiffReview, AddComment, PublishReview, ReloadResponses, CompleteReview, RejectReview
│   ├── listeners/      # Event handlers: ReviewModeListener, ReviewFileWatcher, ReviewFileWatcherStartup
│   └── ui/             # UI overlay: gutter icons, line highlighter, comment popup, tool window, status bar
├── src/main/resources/
│   ├── META-INF/plugin.xml   # Plugin descriptor (extensions, actions, keybindings)
│   └── icons/                # SVG icons for gutter markers
├── src/test/kotlin/          # Unit tests (mirrors main structure)
├── review-cli/               # Standalone CLI for Claude to read/respond to reviews
│   └── src/main/kotlin/com/uber/reviewcli/
│       ├── ReviewCli.kt          # CLI entrypoint (list, show, respond, reply, status)
│       └── ReviewFileSchema.kt   # JSON schema for .review.json files
├── docs/                     # Detailed documentation (see INDEX.md)
├── .claude/commands/
│   └── review-respond.md    # Claude Code skill teaching Claude to use review-cli
├── build.gradle.kts          # Root build: IntelliJ Platform plugin + JaCoCo
├── settings.gradle.kts       # Includes review-cli subproject
└── gradle.properties         # JDK 21, Gradle JVM args
```

## Build & Test

**IMPORTANT**: This machine has a corporate Gradle init script (`~/.gradle/init.gradle` → `/opt/homebrew/opt/uber-developer/share/init.gradle`) that adds `artifactory.uber.internal` to all repositories. This breaks builds when off-VPN. **Always use `-Dgradle.user.home=/tmp/gradle-review-plugin-home`** to bypass it.

```bash
# Build the plugin
./gradlew buildPlugin -Dgradle.user.home=/tmp/gradle-review-plugin-home

# Run unit tests (no IntelliJ Platform instrumentation)
./gradlew unitTest -Dgradle.user.home=/tmp/gradle-review-plugin-home

# Run unit tests with coverage verification (95% minimum)
./gradlew jacocoUnitTestCoverageVerification -Dgradle.user.home=/tmp/gradle-review-plugin-home

# Coverage report (HTML + XML)
./gradlew jacocoUnitTestReport -Dgradle.user.home=/tmp/gradle-review-plugin-home
# Output: build/reports/jacoco/jacocoUnitTestReport/html/index.html

# Build the CLI
./gradlew :review-cli:build -Dgradle.user.home=/tmp/gradle-review-plugin-home

# Run CLI tests
./gradlew :review-cli:test -Dgradle.user.home=/tmp/gradle-review-plugin-home

# Run the CLI directly
./gradlew :review-cli:run --args="list .review/example.review.json" -Dgradle.user.home=/tmp/gradle-review-plugin-home
```

### Build Troubleshooting

If you see `No IntelliJ Platform dependency found` or `artifactory.uber.internal` connection errors:
- **Root cause**: `~/.gradle/init.gradle` (symlink to `/opt/homebrew/opt/uber-developer/share/init.gradle`) prepends Uber's internal Artifactory to all Gradle projects
- **Fix**: Add `-Dgradle.user.home=/tmp/gradle-review-plugin-home` to all Gradle commands
- **Do NOT** try `--offline`, clearing `.gradle/`, or restarting the daemon — those don't help

### Coverage Configuration

JaCoCo coverage verification (95% minimum) **excludes IntelliJ Platform-dependent classes** that cannot be unit tested. The exclude list is in `build.gradle.kts` (`platformDependentExcludes`). When adding new UI/listener classes that depend on IntelliJ APIs (Editor, Project, etc.), add them to this list. See [`docs/TESTING.md`](docs/TESTING.md) for the full exclude list.

## Tech Stack

- **Language**: Kotlin 2.0.21, JVM 21 (Temurin)
- **Build**: Gradle 8.12, IntelliJ Platform Plugin 2.2.1
- **IDE Target**: IntelliJ IDEA 2025.2+ (build 252+), works across all JetBrains IDEs
- **Serialization**: kotlinx-serialization-json 1.7.3
- **IDE Dependencies**: Git4Idea (bundled, diff viewer), org.intellij.plugins.markdown (Markdown editor)
- **Testing**: JUnit 4, JaCoCo (95% coverage minimum)

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Review this Markdown | Ctrl+Shift+R |
| Add Review Comment | Ctrl+Shift+C |
| Publish Review | Ctrl+Shift+P |
| Reload Responses | Ctrl+Shift+L |
| Review the Diff | Ctrl+Shift+D |
| Reply to Review | Ctrl+Shift+Y |

## Claude Integration

The plugin communicates with Claude through `review-cli` and the `/review-respond` skill. See [`docs/CLI.md`](docs/CLI.md) for full details.

```bash
review-cli <command> <file.review.json> [options]

Commands:
  list     List all comments with status
  show     Show full detail for a comment (--comment N)
  respond  Write response for a comment (--comment N --response "...")
  reply    Append reply to a comment (--comment N --text "...")
  status   Show review summary
```

**Skill**: `.claude/commands/review-respond.md` — teaches Claude to process pending comments by calling `review-cli` per-comment.
