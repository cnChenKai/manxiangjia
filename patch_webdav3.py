with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "r") as f:
    content = f.read()

content = content.replace(
    'okhttp3.MediaType.Companion.toMediaTypeOrNull("text/xml")',
    'okhttp3.MediaType.parse("text/xml")'
)

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "w") as f:
    f.write(content)

print("Fixed toMediaTypeOrNull -> MediaType.parse")
