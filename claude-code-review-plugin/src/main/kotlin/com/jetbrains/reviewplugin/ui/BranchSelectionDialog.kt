package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.uber.jetbrains.reviewplugin.services.GitDiffService
import javax.swing.JComponent

class BranchSelectionDialog(
    private val project: Project,
    private val gitDiffService: GitDiffService = GitDiffService(project)
) : DialogWrapper(project) {

    private lateinit var baseBranchCombo: ComboBox<String>
    private lateinit var compareBranchCombo: ComboBox<String>
    private lateinit var changedFilesLabel: JBLabel

    init {
        title = "Review Branch Changes"
        setOKButtonText("Start Review")
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val branches = gitDiffService.getAllBranchNames()
        if (branches.isEmpty()) {
            return JBLabel("No Git repository found.")
        }

        baseBranchCombo = ComboBox(branches.toTypedArray())
        val defaultBase = if ("main" in branches) "main"
            else if ("master" in branches) "master"
            else branches.first()
        baseBranchCombo.selectedItem = defaultBase
        baseBranchCombo.addActionListener { updateChangedFilesCount() }

        compareBranchCombo = ComboBox(branches.toTypedArray())
        val currentBranch = gitDiffService.getCurrentBranchName() ?: branches.first()
        compareBranchCombo.selectedItem = currentBranch
        compareBranchCombo.addActionListener { updateChangedFilesCount() }

        changedFilesLabel = JBLabel("Calculating...")

        updateChangedFilesCount()

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Base branch:", baseBranchCombo)
            .addLabeledComponent("Compare branch:", compareBranchCombo)
            .addComponent(changedFilesLabel)
            .panel
    }

    fun getBaseBranch(): String = baseBranchCombo.selectedItem as String

    fun getCompareBranch(): String = compareBranchCombo.selectedItem as String

    override fun doOKAction() {
        if (getBaseBranch() == getCompareBranch()) {
            Messages.showErrorDialog(
                project,
                "Base and compare branches must be different.",
                "Review the Diff"
            )
            return
        }
        super.doOKAction()
    }

    private fun updateChangedFilesCount() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val base = getBaseBranch()
            val compare = getCompareBranch()
            val stats = gitDiffService.getDiffStats(base, compare)
            ApplicationManager.getApplication().invokeLater {
                changedFilesLabel.text = "Changed: $stats"
            }
        }
    }
}
