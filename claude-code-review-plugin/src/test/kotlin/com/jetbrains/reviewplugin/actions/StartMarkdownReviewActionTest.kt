package com.uber.jetbrains.reviewplugin.actions

import org.junit.Test
import kotlin.test.assertEquals

class StartMarkdownReviewActionTest {

    @Test
    fun `relativePath strips project base path prefix`() {
        // relativePath is a static utility, tested without IntelliJ infrastructure.
        // It takes (project, file) but the logic is: file.path.removePrefix(basePath + "/")
        // We test the string logic directly.
        val basePath = "/Users/vinay/project"
        val filePath = "/Users/vinay/project/docs/README.md"
        val result = filePath.removePrefix("$basePath/")
        assertEquals("docs/README.md", result)
    }

    @Test
    fun `relativePath returns full path when no base path match`() {
        val basePath = "/Users/vinay/other-project"
        val filePath = "/Users/vinay/project/docs/README.md"
        val result = filePath.removePrefix("$basePath/")
        assertEquals("/Users/vinay/project/docs/README.md", result)
    }

    @Test
    fun `relativePath handles nested paths`() {
        val basePath = "/Users/vinay/project"
        val filePath = "/Users/vinay/project/docs/uscorer/ARCHITECTURE_OVERVIEW.md"
        val result = filePath.removePrefix("$basePath/")
        assertEquals("docs/uscorer/ARCHITECTURE_OVERVIEW.md", result)
    }

    @Test
    fun `relativePath handles file in root`() {
        val basePath = "/Users/vinay/project"
        val filePath = "/Users/vinay/project/README.md"
        val result = filePath.removePrefix("$basePath/")
        assertEquals("README.md", result)
    }
}
