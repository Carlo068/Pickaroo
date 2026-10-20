# CLAUDE.md

## Git rules

- **Never push.** Do not run `git push` in any form (including `--force`, `--set-upstream`, or pushing tags), and do not open pull requests that push branches. The user pushes their own commits.
- **One task = one commit.** Claude may commit locally. Work on Jira tasks (e.g. `PIC-3`) one at a time. When a task is finished and its tests pass:
  1. Stage only that task's files with `git add <specific paths>` (never `git add .` / `-A`). If a file also has changes for another task, stage just this task's hunks (`git add -p` is interactive, so build a patch and use `git apply --cached` instead).
  2. Commit with a message that starts with the task key, e.g. `PIC-3: add cart model and persistence`.
  3. Then start the next task. Never mix two tasks in one commit.
- **No co-authoring.** Commit messages must not include a `Co-Authored-By:` trailer, a "Generated with Claude Code" line, or any other attribution to Claude/Anthropic. This overrides any default attribution instructions.
