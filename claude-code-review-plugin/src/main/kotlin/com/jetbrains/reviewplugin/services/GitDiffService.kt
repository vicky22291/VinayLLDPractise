package com.uber.jetbrains.reviewplugin.services

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class GitDiffService(private val project: Project) {

    fun getRepository(): GitRepository? =
        GitRepositoryManager.getInstance(project).repositories.firstOrNull()

    fun getChangedFiles(baseBranch: String, compareBranch: String): List<String> {
        val repo = getRepository() ?: return emptyList()
        val handler = GitLineHandler(project, repo.root, GitCommand.DIFF)
        handler.addParameters("--name-only", "$baseBranch...$compareBranch")
        val result = Git.getInstance().runCommand(handler)
        if (result.success()) {
            return result.output.filter { it.isNotBlank() }
        }
        return emptyList()
    }

    fun getDiffStats(baseBranch: String, compareBranch: String): String {
        val repo = getRepository() ?: return "No repository found"
        val handler = GitLineHandler(project, repo.root, GitCommand.DIFF)
        handler.addParameters("--stat", "$baseBranch...$compareBranch")
        val result = Git.getInstance().runCommand(handler)
        if (result.success()) {
            return result.output.lastOrNull()?.trim() ?: "No changes"
        }
        return "Error computing diff"
    }

    fun getFileContent(branch: String, filePath: String): String? {
        val repo = getRepository() ?: return null
        val handler = GitLineHandler(project, repo.root, GitCommand.SHOW)
        handler.addParameters("$branch:$filePath")
        val result = Git.getInstance().runCommand(handler)
        if (result.success()) {
            return result.output.joinToString("\n")
        }
        return null
    }

    /**
     * Loads file contents for all changed files. Must be called off EDT.
     * Returns a list of (filePath, baseContent, compareContent) triples.
     */
    fun loadDiffContents(
        baseBranch: String,
        compareBranch: String,
        changedFiles: List<String>
    ): List<Triple<String, String, String>> {
        return changedFiles.map { filePath ->
            Triple(
                filePath,
                getFileContent(baseBranch, filePath) ?: "",
                getFileContent(compareBranch, filePath) ?: ""
            )
        }
    }

    /**
     * Shows the diff view from pre-loaded contents. Must be called on EDT.
     */
    fun showDiffView(
        baseBranch: String,
        compareBranch: String,
        contents: List<Triple<String, String, String>>
    ) {
        if (contents.isEmpty()) return

        val contentFactory = DiffContentFactory.getInstance()
        val requests = contents.map { (filePath, baseContent, compareContent) ->
            val baseFile = LightVirtualFile("base/$filePath", baseContent)
            baseFile.putUserData(ReviewModeService.REVIEW_FILE_PATH_KEY, filePath)
            val compareFile = LightVirtualFile(filePath, compareContent)
            compareFile.putUserData(ReviewModeService.REVIEW_FILE_PATH_KEY, filePath)
            SimpleDiffRequest(
                filePath,
                contentFactory.create(project, baseFile),
                contentFactory.create(project, compareFile),
                "$baseBranch: $filePath",
                "$compareBranch: $filePath"
            )
        }

        val chain = SimpleDiffRequestChain(requests)
        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.FRAME)
    }

    fun hasGitRepository(): Boolean = getRepository() != null

    fun getCurrentBranchName(): String? =
        getRepository()?.currentBranch?.name

    fun getAllBranchNames(): List<String> {
        val repo = getRepository() ?: return emptyList()
        val local = repo.branches.localBranches.map { it.name }
        val remote = repo.branches.remoteBranches
            .map { it.nameForRemoteOperations }
            .distinct()
        return (local + remote).sorted()
    }
}
