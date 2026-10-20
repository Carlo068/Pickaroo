#!/usr/bin/env bash
# Create the branch for a Jira task, from the current branch (so earlier unmerged tasks are included).
# Usage: start-task.sh PIC-45 live-driver-map
set -eu
key="${1:?task key, e.g. PIC-45}"
slug="${2:?short-slug, e.g. live-driver-map}"
cd "$(git rev-parse --show-toplevel)"

case "$key" in PIC-[0-9]*) ;; *) echo "task key must look like PIC-45"; exit 64 ;; esac
branch="feature/$key-$slug"

if git rev-parse --verify -q "$branch" >/dev/null; then
  echo "branch $branch already exists"
  exit 1
fi

from="$(git branch --show-current)"
git checkout -q -b "$branch"
echo "created $branch from $from"
# Uncommitted files are carried over; show them so unrelated leftovers are noticed.
git status --short | grep -v '^?? \.idea\|\.idea/' || true
