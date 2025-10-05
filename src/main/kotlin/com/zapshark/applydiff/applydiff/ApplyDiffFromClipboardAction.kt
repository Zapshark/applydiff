package com.zapshark.applydiff

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class ApplyDiffFromClipboardAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val doc = editor.document

        val text = getClipboardText()?.trim()
        if (text.isNullOrEmpty()) {
            Messages.showInfoMessage(project, "Clipboard is empty.", "Apply Diff")
            return
        }

        val diff = DiffParser.parseWithPrompt(project, text)
        if (diff.hunks.isEmpty() && diff.suggestionReplacement == null) {
            Messages.showErrorDialog(project, "No recognizable diff or suggestion found.", "Apply Diff")
            return
        }

        // Determine selection range (fallback to whole doc)
        val selModel = editor.selectionModel
        val selection = if (selModel.hasSelection()) {
            TextRange(selModel.selectionStart, selModel.selectionEnd)
        } else {
            val caret = editor.caretModel.offset
            TextRange(caret, caret) // insert at caret when no selection
        }

        // Build preview text (non-forced by default)
        val build = Preview.buildPatchedText(doc, diff, force = false, selection = selection)

        // Show diff preview
        Preview.show(project, doc.text, build.newText, title = "Preview Clipboard Patch")

        // Confirm apply
        val res = Messages.showYesNoDialog(project, "Apply these changes to the current file?", "Apply Diff", "Apply", "Cancel", null)
        if (res == Messages.YES) {
            apply(project, doc, diff, force = build.failures.isNotEmpty(), selection = selection)
        }
    }

    private fun apply(project: com.intellij.openapi.project.Project, doc: Document, diff: ParsedDiff, force: Boolean, selection: TextRange) {
        WriteCommandAction.runWriteCommandAction(project) {
            HunkApplier.apply(doc, diff, force, selection)
        }

        Messages.showInfoMessage(project, "Applied.", "Apply Diff")
    }

    private fun getClipboardText(): String? = try {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        cb.getData(DataFlavor.stringFlavor) as? String
    } catch (_: Throwable) { null }
}
