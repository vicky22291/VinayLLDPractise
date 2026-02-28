# Task 01: Project Scaffold & Build Configuration

**Phase**: 1 - Foundation
**LOC**: ~120
**Dependencies**: None
**Verification**: `./gradlew build` succeeds, plugin loads in IntelliJ sandbox via `./gradlew runIde`

---

## Objective

Set up the Gradle project structure, build configuration, plugin descriptor skeleton, and icon placeholder files. After this task, the plugin compiles, loads in an IntelliJ sandbox, and is visible in Settings > Plugins.

---

## Files to Create

```
claude-code-review-plugin/
├── build.gradle.kts              # ~50 LOC
├── settings.gradle.kts           # ~5 LOC
├── gradle.properties             # ~10 LOC
├── .gitignore                    # ~15 LOC
└── src/main/resources/
    ├── META-INF/
    │   └── plugin.xml            # ~40 LOC (skeleton)
    └── icons/
        ├── addComment.svg        # placeholder
        ├── commentExists.svg     # placeholder
        ├── commentResolved.svg   # placeholder
        └── reviewMode.svg        # placeholder
```

---

## What to Implement

### `build.gradle.kts`

**Reference**: IMPLEMENTATION.md Section 7

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.uber.jetbrains.reviewplugin"
version = "1.0.0"
```

Key dependencies:
- `intellijIdeaCommunity("2025.2")`
- `bundledPlugin("Git4Idea")`
- `bundledPlugin("org.intellij.plugins.markdown")`
- `kotlinx-serialization-json:1.7.3`
- JVM toolchain: 21

### `settings.gradle.kts`

```kotlin
rootProject.name = "claude-code-review-plugin"
```

### `gradle.properties`

```properties
kotlin.stdlib.default.dependency=false
org.gradle.jvmargs=-Xmx2g
```

### `plugin.xml` (skeleton)

**Reference**: IMPLEMENTATION.md Section 5

Declare:
- Plugin ID: `com.uber.jetbrains.reviewplugin`
- Name: `Claude Code Review`
- Dependencies: `com.intellij.modules.platform`, `Git4Idea`, `org.intellij.plugins.markdown`
- No services, actions, or extensions yet (those come in later tasks)
- Include `<description>` and `<vendor>` tags

### `.gitignore`

Standard Gradle/IntelliJ ignores: `build/`, `.gradle/`, `.idea/`, `*.iml`, `out/`

### SVG Icons

Create 4 minimal SVG placeholders (16x16, simple shapes):
- `addComment.svg` — blue "+" circle
- `commentExists.svg` — blue chat bubble
- `commentResolved.svg` — green checkmark
- `reviewMode.svg` — blue review icon

These can be simple SVGs (~5-10 lines each). They will be refined later but must be valid SVG files that IntelliJ can render.

---

## Verification

1. `./gradlew build` compiles without errors
2. `./gradlew runIde` launches IntelliJ sandbox
3. Plugin appears in Settings > Plugins (search "Claude Code Review")
4. No errors in IDE log related to the plugin
