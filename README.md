# Apply Diff from Clipboard (Current File)

A lightweight plugin for JetBrains IDEs (PHPStorm, IntelliJ IDEA, and others) that lets you **apply diffs or ChatGPT/GitHub suggestion blocks directly to the file you’re editing**.

---

## ✨ Features
- Apply **unified diffs** (`@@ -l,s +l,s @@` style patches) from your clipboard.
- Apply **GitHub ```suggestion``` blocks** to the current selection (or the whole file if no selection).
- Apply **fenced ```diff``` blocks** with `+` and `-` lines, even if no filenames are given.
- Works on the **current editor file only** — no need for VCS patch headers.
- Built-in **preview**: shows a side-by-side diff of *Current* vs *Patched* before applying.
- Configured with a default shortcut:  
  - **Windows/Linux**: `Ctrl + Alt + Shift + D`  
  - **macOS**: `⌘ + ⌥ + ⇧ + D`

---

## 🚀 Usage
1. Copy a diff block to your clipboard. Examples:

   **Unified diff:**
   ```diff
   @@ -2,5 +2,5 @@
   -console.log("hello world");
   +console.log("hello again world");
   ```

   **GitHub suggestion:**
   ```suggestion
   console.log("hello again world");
   ```

   **Fenced diff:**
   ```diff
   -console.log("hello world");
   +console.log("hello again world");
   ```

2. In the editor:
   - Right-click → **Apply Diff from Clipboard (Current File)**  
   - Or press the shortcut.

3. Review the preview diff window.  
   - Click **Apply** to patch the file.  
   - Cancel if you don’t want to apply.

---

## 🛠️ Notes
- For full patch files with filenames, you can still use JetBrains’ built-in:  
  **VCS → Apply Patch from Clipboard**.
- This plugin shines when working with diffs copied from:
  - ChatGPT / other AI assistants,
  - GitHub comments (`suggestion` blocks),
  - Snippets missing file headers.

---

## 📦 Compatibility
- Tested on: **PHPStorm**, **IntelliJ IDEA** (2024.3 baseline).
- Requires: **JDK 21+**.
- Works across most JetBrains IDEs based on the IntelliJ platform.

---

## 🧑‍💻 Author
Created by **ZapShark Technologies**.  
Contributions and feedback welcome!
