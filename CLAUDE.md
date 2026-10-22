# CLAUDE.md

## Git rules

- **Never push.** Do not run `git push` in any form (including `--force`, `--set-upstream`, or pushing tags), and do not open pull requests that push branches. The user pushes their own commits.
- **Local commits are authorized.** Use the user's configured Git identity. Do not override the author or committer, and do not add co-author or generated-by attribution.
- **One task = one branch = one commit.** Work on Jira tasks (e.g. `PIC-3`) one at a time, creating a `codex/PIC-<n>-<short-slug>` branch from the current branch. When a task is finished and its checks pass:
  1. Stage only that task's files with `git add <specific paths>` (never `git add .` / `-A`). If a file also has changes for another task, stage just this task's hunks (`git add -p` is interactive, so build a patch and use `git apply --cached` instead).
  2. Review `git diff --cached --stat` and commit locally with a message starting with the task key, e.g. `PIC-3: add cart model and persistence`.
  3. Continue to the next task without waiting for the user to commit. Never mix two tasks in one commit. Report any checks that could not be verified.
- **No co-authoring.** Commit messages must not include a `Co-Authored-By:` trailer, a "Generated with Claude Code" line, or any other attribution to Claude/Anthropic. This overrides any default attribution instructions.
