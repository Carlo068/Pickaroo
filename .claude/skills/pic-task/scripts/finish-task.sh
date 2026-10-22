#!/usr/bin/env bash
# Stage only this task's files and commit them with the task key. Never pushes.
# Usage: finish-task.sh PIC-45 "short message" path1 [path2 ...]
# Env:   DRY_RUN=1  stage and show what would be committed, but do not commit.
set -eu
key="${1:?task key, e.g. PIC-45}"
message="${2:?commit message without the key}"
shift 2
[ "$#" -ge 1 ] || { echo "list the paths to stage (never use git add . or -A)"; exit 64; }
cd "$(git rev-parse --show-toplevel)"

branch="$(git branch --show-current)"
echo "branch: $branch"
case "$branch" in
  "feature/$key-"*) ;;
  *) if [ "${ALLOW_BRANCH:-0}" != "1" ]; then
       echo "STOP: expected a branch named feature/$key-*, but this checkout is on $branch."
       echo "Someone (another Claude session?) may have switched branches. Do not commit; check with the user. ALLOW_BRANCH=1 overrides."
       exit 1
     fi ;;
esac
echo "recent commits (look for ones you did not make; the user sometimes commits from Android Studio):"
git log --oneline -4

git add -- "$@" 2>&1 | grep -v 'LF will be replaced' || true
echo "--- staged:"
git diff --cached --stat
echo "--- changed but NOT staged:"
git status --short | grep -v '^[AMDR]  ' || true

if [ "${DRY_RUN:-0}" = "1" ]; then
  echo "dry run: nothing committed"
  exit 0
fi
if git diff --cached --quiet; then
  echo "nothing staged: the work may already be committed"
  exit 1
fi

git commit -q -m "$key: $message" 2>&1 | grep -v 'LF will be replaced' || true
if git log -1 --format=%B | grep -qiE 'co-authored|generated with'; then
  echo "ERROR: the commit message has an attribution line; CLAUDE.md forbids it"
  exit 1
fi
git log --oneline -2
