with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "r") as f:
    content = f.read()

search = """            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {"""

replace = """            } else if (uiState.entries.isEmpty() && uiState.isRoot) {
                Text(
                    text = "空目录或没有内容",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {"""

content = content.replace(search, replace)

with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "w") as f:
    f.write(content)

print("Patched RemoteBrowserScreen.kt")
