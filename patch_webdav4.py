with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "r") as f:
    content = f.read()

content = content.replace(
    'okhttp3.MediaType.parse("text/xml")',
    '"text/xml".toMediaType()'
)
content = content.replace(
    'import okhttp3.RequestBody.Companion.toRequestBody',
    'import okhttp3.RequestBody.Companion.toRequestBody\nimport okhttp3.MediaType.Companion.toMediaType'
)

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "w") as f:
    f.write(content)

print("Fixed toMediaType")
