# Task 11: File Watcher + Reload Action + Claude Code Skill

**Phase**: 3 - Claude Integration
**LOC**: ~280
**Dependencies**: Task 04, Task 05, Task 08, Task 10
**Verification**: File watcher detects external changes to `.review.json`, notification appears, "Reload Responses" loads Claude's responses into the session

---

## Objective

Complete the bidirectional loop: detect when Claude/review-cli writes responses to the `.review.json` file, notify the user, and provide a reload action that parses responses and updates the UI. Also create the Claude Code skill that teaches Claude how to use `review-cli`.

After this task, the full Markdown review workflow is end-to-end functional:
1. Add comments -> Publish -> Claude processes via `/review-respond` -> Reload -> See responses inline

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/listeners/
├── ReviewFileWatcher.kt               # ~80 LOC
└── ReviewFileWatcherStartup.kt        # ~50 LOC

src/main/kotlin/com/uber/jetbrains/reviewplugin/actions/
└── ReloadResponsesAction.kt           # ~60 LOC

claude-code-review-plugin/.claude/commands/
└── review-respond.md                  # ~40 LOC (Claude Code skill)
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register startup activity + action
```

---

## What to Implement

### `ReviewFileWatcher` (VirtualFileListener)

**Reference**: HLD Section 9.2, IMPLEMENTATION.md Section 6.7

Watches the `.review/` directory for external modifications (Claude/review-cli writing responses).

```
CLASS ReviewFileWatcher(private val project: Project) : VirtualFileListener

  FUNCTION contentsChanged(event: VirtualFileEvent)
      file = event.file

      // Ignore non-review files
      IF NOT isReviewFile(file): RETURN

      // Ignore our own writes (plugin saves trigger this too)
      // Use a flag or timestamp check to distinguish external vs internal changes
      IF isInternalWrite(file): RETURN

      // Find the session that owns this review file
      reviewModeService = project.service<ReviewModeService>()
      session = reviewModeService.getAllActiveSessions()
          .find { it.reviewFilePath != null AND file.path.endsWith(it.getReviewFileName()) }

      IF session == null: RETURN

      // Show notification on EDT
      ApplicationManager.getApplication().invokeLater {
          val pendingCount = session.getPendingComments().size
          NotificationGroupManager.getInstance()
              .getNotificationGroup("ReviewPlugin")
              .createNotification(
                  "Claude responded. Click to reload responses.",
                  NotificationType.INFORMATION
              )
              .addAction(NotificationAction.createSimple("Reload Responses") {
                  reloadResponses(session)
              })
              .notify(project)
      }

  FUNCTION fileCreated(event: VirtualFileEvent)
      // Also watch for new files created in .review/ (first publish by CLI)
      // Same logic as contentsChanged

  FUNCTION isReviewFile(file: VirtualFile): Boolean
      RETURN file.path.contains("/.review/")
          AND file.name.endsWith(".review.json")
          AND NOT file.path.contains("/.drafts/")
          AND NOT file.path.contains("/archives/")

  FUNCTION reloadResponses(session: ReviewSession)
      IF session.reviewFilePath == null: RETURN
      reviewFileManager = ReviewFileManager()
      reviewFile = reviewFileManager.load(Path.of(session.reviewFilePath))
      commentService = project.service<CommentService>()
      commentService.applyResponses(session, reviewFile)
```

**Internal write detection**: When `ReviewFileManager.publish()` writes the file, set a timestamp or flag in a companion object. `ReviewFileWatcher` checks if the file modification happened within the last 2 seconds of an internal write — if so, ignore it. Alternatively, use `VirtualFile.putUserData()` to mark files being written internally.

### `ReviewFileWatcherStartup` (ProjectActivity)

**Reference**: IMPLEMENTATION.md Section 3.5

Registers the file watcher and restores suspended sessions on project open.

```
CLASS ReviewFileWatcherStartup : ProjectActivity

  FUNCTION execute(project: Project)
      // 1. Register file watcher
      val watcher = ReviewFileWatcher(project)
      VirtualFileManager.getInstance().addVirtualFileListener(watcher, project)
      // Disposable parent = project, auto-cleanup on project close

      // 2. Restore suspended sessions from drafts
      val storageManager = project.service<StorageManager>()
      val reviewModeService = project.service<ReviewModeService>()
      val restoredSessions = storageManager.loadDrafts()

      for (session in restoredSessions) {
          reviewModeService.restoreSuspendedSession(session)
      }

      if (restoredSessions.isNotEmpty()) {
          // Optional: show notification about restored drafts
          NotificationGroupManager.getInstance()
              .getNotificationGroup("ReviewPlugin")
              .createNotification(
                  "${restoredSessions.size} review session(s) restored from previous session.",
                  NotificationType.INFORMATION
              )
              .notify(project)
      }
```

### `ReloadResponsesAction`

**Reference**: HLD Section 12, IMPLEMENTATION.md Section 3.4

Extends `AnAction`. Keyboard shortcut: Ctrl+Shift+L.

```
CLASS ReloadResponsesAction : AnAction("Reload Responses")

  FUNCTION update(e: AnActionEvent)
      project = e.project ?: return
      enabled = project.service<ReviewModeService>()
          .getAllActiveSessions()
          .any { it.reviewFilePath != null AND it.status == PUBLISHED }
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return
      reviewModeService = project.service<ReviewModeService>()

      // Find the published session (could be multiple — reload all)
      sessions = reviewModeService.getAllActiveSessions()
          .filter { it.reviewFilePath != null }

      for (session in sessions) {
          reviewFileManager = ReviewFileManager()
          reviewFile = reviewFileManager.load(Path.of(session.reviewFilePath))
          commentService = project.service<CommentService>()
          commentService.applyResponses(session, reviewFile)
      }

      // Trigger UI refresh
      DaemonCodeAnalyzer.getInstance(project).restart()
```

### Claude Code Skill: `/review-respond`

**Reference**: HLD Section 10.2

File: `.claude/commands/review-respond.md`

```markdown
Process a review file by responding to all pending comments.

## Instructions

1. Run `review-cli list $ARGUMENTS` to see all comments with their status
2. For each comment with status "pending":
   a. Run `review-cli show $ARGUMENTS --comment N` to get the full context
   b. Read the source file at the specified lines to understand the code/document
   c. Research related systems and documentation if the comment requires broader context
   d. Formulate a detailed response with citations (file paths + line numbers)
   e. Run `review-cli respond $ARGUMENTS --comment N --response "your response"`
3. After all pending comments are processed, run `review-cli status $ARGUMENTS` to confirm completion

## Response Guidelines

- **Cite sources**: Always include file paths and line numbers (e.g., `FeatureManager.java:156-234`)
- **Be specific**: Reference actual code, not general concepts
- **Use diagrams**: Include Mermaid diagrams when explaining flows or architecture
- **Stay concise**: Aim for 2-5 paragraphs per response, focused on the question
- **Ask back**: If a comment is unclear, respond with a clarifying question rather than skipping
- **Code suggestions**: When relevant, include specific code changes in diff format

## Example

```bash
# Step 1: See what needs responding
review-cli list .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json

# Step 2: Process each pending comment
review-cli show .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json --comment 1
# ... read source, research ...
review-cli respond .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json --comment 1 --response "Based on..."

# Step 3: Confirm
review-cli status .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json
```
```

### `plugin.xml` Updates

```xml
<!-- Startup Activity -->
<postStartupActivity
    implementation="com.uber.jetbrains.reviewplugin.listeners.ReviewFileWatcherStartup"/>

<!-- Notification Group (if not already registered) -->
<notificationGroup id="ReviewPlugin" displayType="BALLOON"/>

<!-- Reload Action -->
<action id="ReviewPlugin.ReloadResponses"
    class="...ReloadResponsesAction"
    text="Reload Responses"
    description="Reload Claude responses from .review/ file">
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift L"/>
</action>
```

---

## Verification

### File Watcher
1. Publish a review -> manually edit the `.review.json` file to add a `claudeResponse` -> notification appears in IDE
2. Notification has "Reload Responses" action button
3. Click reload -> responses appear in tool window, gutter icons update

### Reload Action
4. Ctrl+Shift+L reloads responses from the published review file
5. After reload: comment statuses update from PENDING to RESOLVED
6. After reload: gutter icons change from yellow clock to green checkmark

### Draft Restore
7. Add 3 draft comments -> close IDE -> reopen -> notification shows "1 review session restored"
8. Enter review mode on same file -> all 3 drafts are present

### Claude Skill
9. `/review-respond .review/test.review.json` triggers Claude to use review-cli commands
10. Claude processes each pending comment and writes responses
