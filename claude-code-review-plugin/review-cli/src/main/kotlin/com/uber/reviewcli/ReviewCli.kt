package com.uber.reviewcli

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

fun main(args: Array<String>) {
    val cli = ReviewCli()
    val result = cli.run(args)
    print(result.output)
    if (!result.success) {
        System.exit(1)
    }
}

data class CliResult(val output: String, val success: Boolean)

class ReviewCli {

    fun run(args: Array<String>): CliResult {
        if (args.isEmpty()) return CliResult(USAGE, false)

        val command = args[0]
        if (command == "--help" || command == "-h") return CliResult(USAGE, true)
        if (args.size < 2) return CliResult(USAGE, false)

        val filePath = Path.of(args[1])
        val flags = parseFlags(args.drop(2))

        return when (command) {
            "list" -> list(filePath)
            "show" -> show(filePath, flags)
            "respond" -> respond(filePath, flags)
            "reply" -> reply(filePath, flags)
            "status" -> status(filePath)
            else -> CliResult("Error: Unknown command: $command\n$USAGE\n", false)
        }
    }

    fun list(filePath: Path): CliResult {
        val reviewFile = loadFile(filePath) ?: return fileNotFound(filePath)

        val sb = StringBuilder()
        for (comment in reviewFile.comments) {
            val statusPad = comment.status.padEnd(8)
            val lines = formatLines(comment.startLine, comment.endLine)
            sb.appendLine("[${comment.index}] $statusPad | ${comment.filePath}:$lines")
            sb.appendLine("    ${truncate(comment.userComment)}")
        }

        val total = reviewFile.comments.size
        val pending = reviewFile.comments.count { it.status == "pending" }
        val resolved = reviewFile.comments.count { it.status == "resolved" }
        sb.appendLine()
        sb.appendLine("Summary: $total total | $pending pending | $resolved resolved")
        return CliResult(sb.toString(), true)
    }

    fun show(filePath: Path, flags: Map<String, String>): CliResult {
        val reviewFile = loadFile(filePath) ?: return fileNotFound(filePath)

        val indexStr = flags["comment"]
            ?: return CliResult("Error: Missing required flag: --comment\n", false)
        val index = indexStr.toIntOrNull()
            ?: return CliResult("Error: Invalid comment index: $indexStr\n", false)
        val comment = reviewFile.comments.find { it.index == index }
            ?: return commentNotFound(index, reviewFile.comments.size)

        val sb = StringBuilder()
        sb.appendLine("Comment $index")
        sb.appendLine("File:    ${comment.filePath}")
        sb.appendLine("Lines:   ${formatLines(comment.startLine, comment.endLine)}")
        sb.appendLine("Status:  ${comment.status}")
        sb.appendLine()
        sb.appendLine("Context:")
        for (line in comment.selectedText.lines()) {
            sb.appendLine("  $line")
        }
        sb.appendLine()
        sb.appendLine("User Comment:")
        sb.appendLine("  ${comment.userComment}")
        sb.appendLine()
        sb.appendLine("Claude Response:")
        if (comment.claudeResponse != null) {
            for (line in comment.claudeResponse.lines()) {
                sb.appendLine("  $line")
            }
        } else {
            sb.appendLine("  (none)")
        }
        sb.appendLine()
        sb.appendLine("Replies: ${comment.replies.size}")
        for (reply in comment.replies) {
            sb.appendLine("  [${reply.author}] ${reply.text}")
        }
        return CliResult(sb.toString(), true)
    }

    fun respond(filePath: Path, flags: Map<String, String>): CliResult {
        val reviewFile = loadFile(filePath) ?: return fileNotFound(filePath)

        val indexStr = flags["comment"]
            ?: return CliResult("Error: Missing required flag: --comment\n", false)
        val index = indexStr.toIntOrNull()
            ?: return CliResult("Error: Invalid comment index: $indexStr\n", false)
        val response = flags["response"]
            ?: return CliResult("Error: Missing required flag: --response\n", false)

        val commentIdx = reviewFile.comments.indexOfFirst { it.index == index }
        if (commentIdx < 0) return commentNotFound(index, reviewFile.comments.size)

        val updated = reviewFile.comments.toMutableList()
        updated[commentIdx] = updated[commentIdx].copy(
            claudeResponse = response,
            status = "resolved"
        )
        val updatedFile = reviewFile.copy(comments = updated)
        writeFile(filePath, updatedFile)

        return CliResult("Comment $index resolved.\n", true)
    }

    fun reply(filePath: Path, flags: Map<String, String>): CliResult {
        val reviewFile = loadFile(filePath) ?: return fileNotFound(filePath)

        val indexStr = flags["comment"]
            ?: return CliResult("Error: Missing required flag: --comment\n", false)
        val index = indexStr.toIntOrNull()
            ?: return CliResult("Error: Invalid comment index: $indexStr\n", false)
        val text = flags["text"]
            ?: return CliResult("Error: Missing required flag: --text\n", false)

        val commentIdx = reviewFile.comments.indexOfFirst { it.index == index }
        if (commentIdx < 0) return commentNotFound(index, reviewFile.comments.size)

        val comment = reviewFile.comments[commentIdx]
        val newReply = Reply(
            author = "user",
            timestamp = Instant.now().toString(),
            text = text
        )
        val updated = reviewFile.comments.toMutableList()
        updated[commentIdx] = comment.copy(
            replies = comment.replies + newReply,
            status = "pending"
        )
        val updatedFile = reviewFile.copy(comments = updated)
        writeFile(filePath, updatedFile)

        return CliResult("Reply added to comment $index.\n", true)
    }

    fun status(filePath: Path): CliResult {
        val reviewFile = loadFile(filePath) ?: return fileNotFound(filePath)

        val sb = StringBuilder()
        sb.appendLine("Review: ${filePath.fileName}")
        sb.appendLine("Type:   ${reviewFile.type}")

        when (reviewFile.type) {
            "MARKDOWN" -> sb.appendLine("Source: ${reviewFile.metadata.sourceFile ?: "unknown"}")
            "GIT_DIFF" -> sb.appendLine("Diff:   ${reviewFile.metadata.baseBranch} -> ${reviewFile.metadata.compareBranch}")
        }
        sb.appendLine()

        val total = reviewFile.comments.size
        val pending = reviewFile.comments.count { it.status == "pending" }
        val resolved = reviewFile.comments.count { it.status == "resolved" }
        val skipped = reviewFile.comments.count { it.status == "skipped" }
        sb.appendLine("Total: $total | Pending: $pending | Resolved: $resolved | Skipped: $skipped")
        return CliResult(sb.toString(), true)
    }

    // --- Helpers ---

    private fun loadFile(filePath: Path): ReviewFile? {
        if (!Files.exists(filePath)) return null
        return try {
            ReviewFile.fromJson(Files.readString(filePath))
        } catch (e: Exception) {
            null
        }
    }

    private fun writeFile(filePath: Path, reviewFile: ReviewFile) {
        Files.writeString(filePath, reviewFile.toJson())
    }

    private fun fileNotFound(filePath: Path): CliResult =
        CliResult("Error: File not found: $filePath\n", false)

    private fun commentNotFound(index: Int, max: Int): CliResult =
        CliResult("Error: Comment $index not found. Valid range: 1-$max\n", false)

    companion object {
        const val MAX_PREVIEW = 80

        val USAGE = """
            |Usage: review-cli <command> <file.review.json> [options]
            |
            |Commands:
            |  list     List all comments with status
            |  show     Show full detail for a comment (--comment N)
            |  respond  Write response for a comment (--comment N --response "...")
            |  reply    Append reply to a comment (--comment N --text "...")
            |  status   Show review summary
        """.trimMargin() + "\n"

        fun truncate(text: String, maxLen: Int = MAX_PREVIEW): String {
            val oneLine = text.replace('\n', ' ').trim()
            if (oneLine.length <= maxLen) return oneLine
            return oneLine.take(maxLen) + "..."
        }

        fun formatLines(start: Int, end: Int): String =
            if (start == end) "$start" else "$start-$end"

        fun parseFlags(args: List<String>): Map<String, String> {
            val flags = mutableMapOf<String, String>()
            var i = 0
            while (i < args.size) {
                val arg = args[i]
                if (arg.startsWith("--") && i + 1 < args.size) {
                    flags[arg.removePrefix("--")] = args[i + 1]
                    i += 2
                } else {
                    i++
                }
            }
            return flags
        }
    }
}
