with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "r") as f:
    content = f.read()

# Remove the extra quotes from the XML body
content = content.replace(
    '"""<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""',
    '"""<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""'
)

# Actually there were four double quotes in the regex replacement output earlier.
# Let's clean it up properly.
content = content.replace(
    '""""<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""',
    '"""<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""'
)

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "w") as f:
    f.write(content)

print("Cleaned up quotes in WebDavSourceClient.kt")
