with open("data-files/src/test/kotlin/com/mangahaven/data/files/remote/WebDavSourceClientTest.kt", "r") as f:
    content = f.read()

import re
# We just replace `fun testErrorResponseThrowsException() = runBlocking {` with `fun testErrorResponseThrowsException() { runBlocking {`
# and add a closing brace at the end of the method
content = re.sub(
    r'fun testErrorResponseThrowsException\(\) = runBlocking \{',
    'fun testErrorResponseThrowsException() { runBlocking {',
    content
)
content = re.sub(
    r'fun testParsePropfindResponse\(\) = runBlocking \{',
    'fun testParsePropfindResponse() { runBlocking {',
    content
)

# And add the closing brace to the end of the class right before the last closing brace
# Actually, the easiest is to just find the last closing brace of each test and insert a new one
# Since each test body is indented by 8 spaces, and the `}` is indented by 4 spaces.
content = re.sub(
    r'^    \}(\n\n    @Test)',
    r'    } }\1',
    content,
    flags=re.MULTILINE
)
content = re.sub(
    r'^    \}(\n\})',
    r'    } }\1',
    content,
    flags=re.MULTILINE
)

with open("data-files/src/test/kotlin/com/mangahaven/data/files/remote/WebDavSourceClientTest.kt", "w") as f:
    f.write(content)
