# Task 12: Git Diff Review — Branch Selection + Diff View Integration

**Phase**: 4 - Git Diff Review
**LOC**: ~450
**Dependencies**: Task 04, Task 05, Task 06, Task 07, Task 08, Task 09
**Verification**: VCS menu shows "Review the Diff". Branch picker opens, user selects branches, diff view opens with comment overlay.

---

## Objective

Implement the Git diff review mode: the `StartDiffReviewAction`, `BranchSelectionDialog`, and Git4Idea API integration. After this task, users can review branch changes with the same comment/publish/reload workflow as Markdown review. All existing infrastructure (gutter icons, tool window, publish, reload) is reused.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/actions/
└── StartDiffReviewAction.kt          # ~100 LOC

src/main/kotlin/com/uber/jetbrains/reviewplugin/ui/
└── BranchSelectionDialog.kt          # ~200 LOC

src/main/kotlin/com/uber/jetbrains/reviewplugin/services/
└── GitDiffService.kt                 # ~150 LOC (Git4Idea API wrapper)
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register diff action
```

---

## What to Implement

### `StartDiffReviewAction`

**Reference**: HLD Section 12, IMPLEMENTATION.md Section 3.4

Extends `AnAction`. Registered in `VcsGroups`. Keyboard shortcut: Ctrl+Shift+D.

```
CLASS StartDiffReviewAction : AnAction("Review the Diff")

  FUNCTION update(e: AnActionEvent)
      project = e.project
      visible = project != null AND hasGitRepository(project)
      enabled = visible AND NOT hasDiffReviewActive(project)
      e.presentation.isVisible = visible
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return

      // Open branch selection dialog
      dialog = BranchSelectionDialog(project)
      IF NOT dialog.showAndGet(): RETURN  // user cancelled

      baseBranch = dialog.getBaseBranch()
      compareBranch = dialog.getCompareBranch()

      // Compute diff and get changed files
      gitDiffService = GitDiffService(project)
      changedFiles = gitDiffService.getChangedFiles(baseBranch, compareBranch)

      IF changedFiles.isEmpty():
          Messages.showInfoMessage(project, "No changes between branches.", "Review the Diff")
          RETURN

      // Enter diff review mode
      reviewModeService = project.service<ReviewModeService>()
      session = reviewModeService.enterDiffReview(baseBranch, compareBranch, changedFiles)

      // Open diff view
      gitDiffService.openDiffView(baseBranch, compareBranch)

      // Trigger gutter refresh and open tool window
      DaemonCodeAnalyzer.getInstance(project).restart()
      ToolWindowManager.getInstance(project).getToolWindow("Claude Code Review")?.show()

  FUNCTION hasGitRepository(project: Project): Boolean
      return GitRepositoryManager.getInstance(project).repositories.isNotEmpty()

  FUNCTION hasDiffReviewActive(project: Project): Boolean
      return project.service<ReviewModeService>().getAllActiveSessions()
          .any { it is GitDiffReviewSession }
```

### `BranchSelectionDialog`

**Reference**: HLD Section 8.1, IMPLEMENTATION.md Section 3.3

Extends `DialogWrapper`.

**Layout:**
```
+-------------------------------------------------------+
|  Review Branch Changes                                 |
+-------------------------------------------------------+
|                                                        |
|  Base branch:    [main              ▼]                 |
|  Compare branch: [feature-auth      ▼]                 |
|                                                        |
|  Changed files: 5 (+247 -89)                           |
|                                                        |
+-------------------------------------------------------+
|                           [Cancel]  [Start Review]     |
+-------------------------------------------------------+
```

```
CLASS BranchSelectionDialog(private val project: Project) : DialogWrapper(project)

  // UI components
  baseBranchCombo: ComboBox<String>
  compareBranchCombo: ComboBox<String>
  changedFilesLabel: JBLabel

  FUNCTION init()
      title = "Review Branch Changes"
      setOKButtonText("Start Review")
      init()  // DialogWrapper.init()

  FUNCTION createCenterPanel(): JComponent
      // Load branches
      repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
      IF repo == null: show error, return empty panel

      branches = getAllBranches(repo)

      // Base branch dropdown
      baseBranchCombo = ComboBox(branches.toTypedArray())
      baseBranchCombo.selectedItem = "main"  // default
      baseBranchCombo.addActionListener { updateChangedFilesCount() }

      // Compare branch dropdown — default to current branch
      compareBranchCombo = ComboBox(branches.toTypedArray())
      currentBranch = repo.currentBranch?.name ?: branches.firstOrNull()
      compareBranchCombo.selectedItem = currentBranch
      compareBranchCombo.addActionListener { updateChangedFilesCount() }

      // Changed files label
      changedFilesLabel = JBLabel("Calculating...")

      // Layout using FormBuilder
      panel = FormBuilder.createFormBuilder()
          .addLabeledComponent("Base branch:", baseBranchCombo)
          .addLabeledComponent("Compare branch:", compareBranchCombo)
          .addComponent(changedFilesLabel)
          .panel

      // Initial count
      updateChangedFilesCount()

      RETURN panel

  FUNCTION getAllBranches(repo: GitRepository): List<String>
      localBranches = repo.branches.localBranches.map { it.name }
      remoteBranches = repo.branches.remoteBranches
          .map { it.nameForRemoteOperations }
          .distinct()
      RETURN (localBranches + remoteBranches).sorted()

  FUNCTION updateChangedFilesCount()
      // Run on background thread to avoid UI freeze
      ApplicationManager.getApplication().executeOnPooledThread {
          base = getBaseBranch()
          compare = getCompareBranch()
          gitDiffService = GitDiffService(project)
          stats = gitDiffService.getDiffStats(base, compare)
          ApplicationManager.getApplication().invokeLater {
              changedFilesLabel.text = stats  // e.g., "5 files changed (+247 -89)"
          }
      }

  FUNCTION getBaseBranch(): String = baseBranchCombo.selectedItem as String
  FUNCTION getCompareBranch(): String = compareBranchCombo.selectedItem as String

  FUNCTION doOKAction()
      IF getBaseBranch() == getCompareBranch():
          Messages.showErrorDialog("Base and compare branches must be different.", "Error")
          RETURN
      super.doOKAction()
```

### `GitDiffService` (utility wrapper around Git4Idea)

**Reference**: HLD Section 8.2-8.3

This is NOT a project service — it's a utility class that wraps Git4Idea API calls.

```
CLASS GitDiffService(private val project: Project)

  FUNCTION getChangedFiles(baseBranch: String, compareBranch: String): List<String>
      // Uses GitLineHandler to run: git diff --name-only baseBranch...compareBranch
      repo = getRepository() ?: return emptyList()
      handler = GitLineHandler(project, repo.root, GitCommand.DIFF)
      handler.addParameters("--name-only", "$baseBranch...$compareBranch")
      result = Git.getInstance().runCommand(handler)
      IF result.success():
          RETURN result.output.filter { it.isNotBlank() }
      RETURN emptyList()

  FUNCTION getDiffStats(baseBranch: String, compareBranch: String): String
      // Uses GitLineHandler to run: git diff --stat baseBranch...compareBranch
      repo = getRepository() ?: return "No repository found"
      handler = GitLineHandler(project, repo.root, GitCommand.DIFF)
      handler.addParameters("--stat", "$baseBranch...$compareBranch")
      result = Git.getInstance().runCommand(handler)
      IF result.success():
          // Last line of --stat output is the summary
          RETURN result.output.lastOrNull()?.trim() ?: "No changes"
      RETURN "Error computing diff"

  FUNCTION openDiffView(baseBranch: String, compareBranch: String)
      // Use IntelliJ's DiffManager to open a multi-file diff view
      repo = getRepository() ?: return
      root = repo.root

      // Get list of Change objects
      changes = computeChanges(baseBranch, compareBranch, root)
      IF changes.isEmpty(): return

      // Create DiffRequestChain from changes
      // Use ChangesUtil or VcsLogUtil to create diff requests
      diffRequestChain = createDiffRequestChain(project, changes)
      DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.DEFAULT)

  FUNCTION computeChanges(baseBranch: String, compareBranch: String, root: VirtualFile): List<Change>
      // Use git4idea API to compute actual Change objects
      // This provides the content for both sides of the diff
      handler = GitLineHandler(project, root, GitCommand.DIFF)
      handler.addParameters("$baseBranch...$compareBranch")
      // Parse diff output into Change objects
      // OR use GitChangeUtils if available

      // Simplified approach: use GitHistoryUtils or GitChangeUtils
      // to get changes between two refs
      RETURN GitChangeUtils.getDiffWithWorkingTree(project, root, baseBranch) ?: emptyList()

  FUNCTION getRepository(): GitRepository?
      return GitRepositoryManager.getInstance(project).repositories.firstOrNull()
```

**Note on diff computation**: The exact Git4Idea API for computing changes between branches varies by IntelliJ version. Key classes to investigate:
- `GitChangeUtils`
- `GitHistoryUtils.history()`
- `VcsLogUtil`
- `ChangesUtil.getChanges()`

The implementation should try `GitChangeUtils` first. If unavailable, fall back to parsing `git diff` output.

### `plugin.xml` Update

```xml
<action id="ReviewPlugin.StartDiffReview"
    class="...StartDiffReviewAction"
    text="Review the Diff"
    description="Review branch changes with inline comments">
    <add-to-group group-id="VcsGroups" anchor="last"/>
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift D"/>
</action>
```

---

## How Diff Comments Work with Existing Infrastructure

The gutter icon provider (Task 06) already works on **any editor**, including the diff viewer. When `ReviewModeService.isInReviewMode(filePath)` returns true for a file in the diff view, "+" icons appear automatically.

For diff reviews, `isInReviewMode()` checks if the file path is in `session.changedFiles`. The comment's `changeType` is set based on the diff context (ADDED, MODIFIED, DELETED).

Comments are anchored to **new-version line numbers** (right side of split view). The `filePath` in each comment is the relative path within the project.

---

## Verification

1. VCS menu shows "Review the Diff" when project has a Git repo
2. "Review the Diff" is hidden when no Git repo exists
3. Click action -> BranchSelectionDialog opens with branch dropdowns
4. Base branch defaults to "main", compare defaults to current branch
5. Changed files count updates when branches are changed
6. "Start Review" creates a `GitDiffReviewSession` and opens diff view
7. Diff view shows split view with changed files
8. "+" gutter icons appear on changed lines in the diff view
9. Clicking "+" opens comment popup -> comment is created with `changeType`
10. Publish generates `.review/diff-main--feature-auth.review.json` with correct metadata
