import re

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "r") as f:
    content = f.read()

# Fix list() failure handling
content = re.sub(
    r'if \(!response\.isSuccessful\) \{[\s\n]*Timber\.e\("WebDAV list failed: \$\{response\.code\} \$\{response\.message\}"\)[\s\n]*return@use emptyList\(\)[\s\n]*\}',
    'if (!response.isSuccessful) {\n                throw java.io.IOException("WebDAV list failed: ${response.code} ${response.message}")\n            }',
    content
)

# Fix stat() failure handling
content = re.sub(
    r'if \(!response\.isSuccessful\) return@use null',
    'if (!response.isSuccessful) throw java.io.IOException("WebDAV stat failed: ${response.code} ${response.message}")',
    content
)

# Add allprop XML body and Content-Type to PROPFIND request
# okhttp3.MediaType.Companion.toMediaType
# "text/xml".toMediaType()

xml_body = '"""<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""'
content = content.replace(
    '".toRequestBody()',
    f'{xml_body}.toRequestBody(okhttp3.MediaType.Companion.toMediaTypeOrNull("text/xml"))'
)

# In parsePropfindResponse, throw exception instead of logging and returning empty
content = re.sub(
    r'catch \(e: Exception\) \{[\s\n]*Timber\.e\(e, "Failed to parse WebDAV XML"\)[\s\n]*\}',
    'catch (e: Exception) {\n            throw java.io.IOException("Failed to parse WebDAV XML", e)\n        }',
    content
)

# We also need to fix URL decoder
content = content.replace(
    'val name = normalizedHref.substringAfterLast(\'/\')\n\n                    entries.add(\n                        SourceEntry(\n                            name = java.net.URLDecoder.decode(name, "UTF-8"),',
    'val name = normalizedHref.substringAfterLast(\'/\')\n                    val decodedName = try {\n                        java.net.URLDecoder.decode(name, "UTF-8")\n                    } catch (e: Exception) { name }\n\n                    entries.add(\n                        SourceEntry(\n                            name = decodedName,'
)

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "w") as f:
    f.write(content)

print("Patched WebDavSourceClient.kt")
