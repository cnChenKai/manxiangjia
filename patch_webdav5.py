import re

with open("data-files/src/main/kotlin/com/mangahaven/data/files/remote/WebDavSourceClient.kt", "r") as f:
    content = f.read()

# Make sure href decode uses URLDecoder properly, but only on the file name.
# It seems my previous patch might have left name decode broken if it's already there.
# Let's see what is there exactly

print(content)
