with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "r") as f:
    content = f.read()

# Revert previous change
search = """            } else if (uiState.entries.isEmpty() && uiState.isRoot) {
                Text(
                    text = "空目录或没有内容",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {"""

replace = """            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.entries.isEmpty() && uiState.isRoot) {
                        item {
                            Text(
                                text = "空目录或没有内容",
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }"""

content = content.replace(search, replace)

# Add import for TextAlign
if "androidx.compose.ui.text.style.TextAlign" not in content:
    content = content.replace(
        "import androidx.compose.ui.Modifier",
        "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.text.style.TextAlign"
    )
    content = content.replace(
        "textAlign = androidx.compose.ui.text.style.TextAlign.Center",
        "textAlign = TextAlign.Center"
    )

with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "w") as f:
    f.write(content)

print("Patched RemoteBrowserScreen.kt logic")
