import base64
import zlib
import re

def kroki_encode(text):
    compressed = zlib.compress(text.encode('utf-8'), 9)
    return base64.urlsafe_b64encode(compressed).decode('utf-8')

text = """graph TD
    A-->B"""

print("https://kroki.io/mermaid/png/" + kroki_encode(text))
