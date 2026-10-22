"""Find a visible text on a `uiautomator dump` of the screen (read from stdin).

Usage: adb exec-out cat /sdcard/ui.xml | python find_node.py [TEXT]
Prints "x y" (the centre of the first match, exact matches first). With no TEXT, succeeds if the screen has any text.
Exit code 0 found, 1 not found.
"""
import html
import re
import sys

# The dump is UTF-8; Windows would otherwise decode stdin as cp1252 and accented texts would never match.
xml = sys.stdin.buffer.read().decode("utf-8", errors="replace")
wanted = sys.argv[1] if len(sys.argv) > 1 else None


def attribute(tag, name):
    found = re.search(rf' {re.escape(name)}="([^"]*)"', tag)
    return html.unescape(found.group(1)) if found else ""


exact, partial, anything = [], [], False
for node in re.finditer(r"<node\b[^>]*>", xml):
    tag = node.group(0)
    labels = [attribute(tag, "text"), attribute(tag, "content-desc")]
    bounds = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", attribute(tag, "bounds"))
    if not bounds or not any(labels):
        continue
    x1, y1, x2, y2 = map(int, bounds.groups())
    if x2 <= x1 or y2 <= y1:
        continue
    anything = True
    centre = ((x1 + x2) // 2, (y1 + y2) // 2)
    if wanted is None:
        continue
    if wanted in labels:
        exact.append(centre)
    elif any(wanted in label for label in labels):
        partial.append(centre)

if wanted is None:
    sys.exit(0 if anything else 1)
hits = exact or partial
if not hits:
    sys.exit(1)
print(*hits[0])
