with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.ui.text.style.TextAlign" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.text.style.TextAlign")

with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "w") as f:
    f.write(content)
