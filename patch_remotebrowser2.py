with open("feature-library/src/main/kotlin/com/mangahaven/feature/library/browser/RemoteBrowserScreen.kt", "r") as f:
    content = f.read()

# Make it support empty state even in subdirectories
content = content.replace(
    "} else if (uiState.entries.isEmpty() && uiState.isRoot) {",
    "} else if (uiState.entries.isEmpty()) {"
)

# And if it's not root, we might want to still show ".." ?
# Let's see: if it's not root, it won't show ".. " if it enters this branch.
# Oh, we should probably handle that. Let's change the layout completely for empty state inside the LazyColumn.
