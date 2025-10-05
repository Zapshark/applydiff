# Apply Diff to Current File — Features

A tiny IntelliJ/IDEA plugin that takes a diff/suggestion from your clipboard, shows a preview, and applies it to the current editor (or selection).

---

## 🚀 Core Flow
1. Copy a diff/suggestion block.
2. Right-click in the editor → **Apply Diff from Clipboard (Current File)**  
   *(or press **Ctrl+Alt+Shift+D**)*.
3. See **Preview** → click **Apply**.

---

## 📋 Clipboard Formats Supported
- **Fenced selection diffs**: code fences like ```diff …``` or `~~~ patch` containing `+`/`-` lines.
- **Unified hunks**: `@@ -oldStart,oldCount +newStart,newCount @@` with `+/-/ ` lines.
- **GitHub suggestions**: ```suggestion …``` (replaces the selection verbatim).
- **Plain `+/-` blocks** (no fence): quick selection patches.
- **Multiple blocks** in a single paste (parsed cumulatively).
- Fences can appear **anywhere** in the text; supports both ``` and ~~~.
- Recognized fence “languages”: **diff**, **patch**, **udiff**, **git-diff**, **unidiff** (anything containing “diff”).
- Ignores “git noise”: `diff --git`, `index …`, `--- a/…`, `+++ b/…`, `No newline at end of file`.
- Normalizes newlines (CRLF/LF) and strips BOM.

---

## ✂️ Selection & Caret Behavior
- **With selection**: selection diffs/suggestions replace **only the selection**.
- **No selection**: selection diffs **insert at the caret** (zero-length range), not the whole file.
- **Unified hunks**: apply by **line numbers**; selection not required.

---

## 👀 Preview
- Side-by-side **Diff Viewer** (Current ⟷ Patched).
- **Indent-tolerant matching** for unified hunks:
    - If “old” lines differ only by leading spaces/tabs, preview still accepts the hunk.
    - **Re-indents** the “new” lines to match your file’s indentation.
- Shows validation notes for hunks that won’t apply; you can still **force** on apply.

---

## ✅ Apply
- Mirrors preview’s indent-tolerant logic (same re-indent rules).
- Safely applies **multiple edits** (sorted to avoid shifting).
- Selection diffs/suggestions: replace selection or insert at caret.
- Unified hunks: line-located replacements.

---

## 🛟 Error Handling & Recovery
- If parsing fails, the plugin offers an **Auto-clean** prompt:
    - Strips code fences and leading `+ / - / >` markers.
    - Retries parsing; if still unclear, creates a **selection hunk** from `+/-` lines.
- Gracefully ignores extra explanatory text around fenced blocks.

---

## 🧩 IntelliJ Integration
- Editor popup action + **keyboard shortcut** (**Ctrl+Alt+Shift+D**).
- One-time **“What’s New”** notification after updates with a link to release notes.

---

## 🏗️ Build & Marketplace
- Uses **CHANGELOG.md → change notes**: Gradle injects the latest entry into Marketplace/Plugins UI “What’s New.”
- No `<change-notes>` needed in `plugin.xml` (handled at build time).

---

## ⚠️ Notes & Limits
- Unified hunks rely on **line numbers**; large file shifts may require “Force apply.”
- Overlapping hunks in the same paste aren’t auto-merged (standard diffs are fine).
- Auto-clean keeps indentation and content—only strips fences and leading markers.

---
