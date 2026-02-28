# Task 13: Session Lifecycle — Complete/Reject Actions + Archive

**Phase**: 5 - Lifecycle Completion
**LOC**: ~200
**Dependencies**: Task 04, Task 08 (tool window buttons), Task 03 (StorageManager.archiveReviewFile)
**Verification**: "Complete" archives review file, clears session, frees name. "Reject" does the same. Archived files appear in `.review/archives/`.

---

## Objective

Implement the terminal session states: `CompleteReviewAction` and `RejectReviewAction`. When a review is completed or rejected, the published `.review.json` file is moved to `.review/archives/` with a random suffix, draft files are cleaned up, the session is removed from active reviews, and the deterministic file name is freed for future reviews of the same file.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/actions/
├── CompleteReviewAction.kt        # ~70 LOC
└── RejectReviewAction.kt         # ~70 LOC
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register both actions
src/main/kotlin/.../ui/ReviewToolWindowPanel.kt  # Wire buttons (if not already done in Task 08)
```

---

## What to Implement

### `CompleteReviewAction`

**Reference**: HLD Section 3.2, 12

Extends `AnAction`. Available from:
- Tool window "Complete" button
- Actions menu (can be invoked via Ctrl+Shift+A search)

```
CLASS CompleteReviewAction : AnAction("Complete Review")

  FUNCTION update(e: AnActionEvent)
      project = e.project
      enabled = project != null AND hasActiveOrPublishedSession(project)
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return
      reviewModeService = project.service<ReviewModeService>()

      // Get the active session (if multiple, use the first — or show chooser)
      session = reviewModeService.getAllActiveSessions().firstOrNull() ?: return

      // Confirm with user
      pendingCount = session.getPendingComments().size
      IF pendingCount > 0:
          confirmed = Messages.showYesNoDialog(
              project,
              "$pendingCount comment(s) are still pending. Complete anyway?",
              "Complete Review",
              Messages.getQuestionIcon()
          )
          IF confirmed != Messages.YES: RETURN

      // Complete the review
      reviewModeService.completeReview(session)

      // Trigger UI cleanup
      DaemonCodeAnalyzer.getInstance(project).restart()

      // Notification
      NotificationGroupManager.getInstance()
          .getNotificationGroup("ReviewPlugin")
          .createNotification(
              "Review completed and archived.",
              NotificationType.INFORMATION
          )
          .notify(project)

  FUNCTION hasActiveOrPublishedSession(project: Project): Boolean
      return project.service<ReviewModeService>()
          .getAllActiveSessions()
          .any { it.status == ACTIVE || it.status == PUBLISHED }
```

### `RejectReviewAction`

**Reference**: HLD Section 3.2, 12

Same structure as `CompleteReviewAction` but with different messaging:

```
CLASS RejectReviewAction : AnAction("Reject Review")

  FUNCTION update(e: AnActionEvent)
      // Same as CompleteReviewAction

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return
      reviewModeService = project.service<ReviewModeService>()
      session = reviewModeService.getAllActiveSessions().firstOrNull() ?: return

      // Confirm with user
      commentCount = session.comments.size
      confirmed = Messages.showYesNoDialog(
          project,
          "Reject this review? $commentCount comment(s) will be archived.",
          "Reject Review",
          Messages.getWarningIcon()
      )
      IF confirmed != Messages.YES: RETURN

      // Reject the review
      reviewModeService.rejectReview(session)

      // Trigger UI cleanup
      DaemonCodeAnalyzer.getInstance(project).restart()

      // Notification
      NotificationGroupManager.getInstance()
          .getNotificationGroup("ReviewPlugin")
          .createNotification(
              "Review rejected and archived.",
              NotificationType.INFORMATION
          )
          .notify(project)
```

### What Happens During Complete/Reject (in ReviewModeService, already implemented in Task 04)

For reference, `ReviewModeService.completeReview()` does:

```
1. session.status = COMPLETED (or REJECTED)
2. StorageManager.archiveReviewFile(session)
   → Moves .review/name.review.json → .review/archives/name-{5char}.review.json
3. StorageManager.deleteDrafts(session.id)
   → Deletes .review/.drafts/session-{uuid}.json
4. activeReviews.remove(sessionKey)
5. Notify listeners: onReviewModeExited(session)
   → Tool window clears
   → Gutter icons disappear
   → Status bar resets
   → Line highlights removed
```

### Tool Window Integration

Ensure `ReviewToolWindowPanel` (Task 08) wires the "Complete" and "Reject" buttons:

```kotlin
// In ReviewToolWindowPanel footer
val completeButton = JButton("Complete ✓").apply {
    addActionListener {
        ActionManager.getInstance()
            .getAction("ReviewPlugin.CompleteReview")
            .actionPerformed(createAnActionEvent())
    }
}

val rejectButton = JButton("Reject ✗").apply {
    addActionListener {
        ActionManager.getInstance()
            .getAction("ReviewPlugin.RejectReview")
            .actionPerformed(createAnActionEvent())
    }
}
```

These buttons should be:
- **Visible**: When a session is ACTIVE or PUBLISHED
- **Hidden**: When no session is active
- **Disabled**: When session is SUSPENDED

### `plugin.xml` Updates

```xml
<action id="ReviewPlugin.CompleteReview"
    class="...CompleteReviewAction"
    text="Complete Review"
    description="Mark review as complete and archive the review file"/>

<action id="ReviewPlugin.RejectReview"
    class="...RejectReviewAction"
    text="Reject Review"
    description="Reject review and archive the review file"/>
```

---

## Archive Behavior Verification

Given this state:
```
.review/
├── .drafts/
│   └── session-abc123.json
└── docs--uscorer--ARCHITECTURE_OVERVIEW.review.json
```

After "Complete Review":
```
.review/
├── .drafts/
│   (empty — session-abc123.json deleted)
└── archives/
    └── docs--uscorer--ARCHITECTURE_OVERVIEW-a3k9m.review.json
```

The name `docs--uscorer--ARCHITECTURE_OVERVIEW.review.json` is now freed. A new review on the same file reuses the same name.

---

## Verification

1. **Complete with all resolved**: All comments resolved -> click "Complete" -> review archived, session cleared
2. **Complete with pending**: Some comments still pending -> confirmation dialog warns -> proceed archives anyway
3. **Reject**: Click "Reject" -> confirmation -> review archived with original content preserved
4. **Archive location**: After complete/reject, check `.review/archives/` contains the file with random suffix
5. **Name reuse**: After archival, start a new review on the same file -> new `.review.json` uses the same deterministic name
6. **Draft cleanup**: After complete/reject, `.review/.drafts/` no longer contains the session file
7. **UI cleanup**: After complete/reject:
   - Gutter icons disappear
   - Tool window shows "no active review"
   - Status bar clears
   - Line highlights removed
8. **Action visibility**: "Complete" and "Reject" are disabled when no active session exists
