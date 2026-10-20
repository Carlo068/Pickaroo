"""Summarize Gradle test result XML files: counts, failures, and runs that died before finishing.

Usage: python summarize_results.py unit|instrumented
Exit code: 0 all passed, 1 failures or no tests found.
"""
import glob
import re
import sys

PATTERNS = {
    "unit": "app/build/test-results/testDebugUnitTest/*.xml",
    "instrumented": "app/build/outputs/androidTest-results/connected/**/*.xml",
}

kind = sys.argv[1] if len(sys.argv) > 1 else "unit"
total = failed = 0

for path in glob.glob(PATTERNS[kind], recursive=True):
    text = open(path, encoding="utf-8").read()
    for case in re.finditer(r'<testcase name="([^"]+)" classname="([^"]+)"[^>]*?(/>|>(.*?)</testcase>)', text, re.S):
        total += 1
        body = case.group(4) or ""
        if "<failure" in body or "<error" in body:
            failed += 1
            reason = re.sub(r"\s+", " ", body)[:300]
            print(f"FAIL {case.group(2).split('.')[-1]}.{case.group(1)}: {reason}")
    for crash in re.finditer(r"Test run failed[^<]*", text):
        failed += 1
        print(f"RUN DIED: {crash.group(0).strip()}")

print(f"{kind}: {total - failed if failed <= total else 0}/{total} passed")
sys.exit(0 if failed == 0 and total > 0 else 1)
