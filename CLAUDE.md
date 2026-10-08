# CLAUDE.md

## Git rules

- **Never push.** Do not run `git push` in any form (including `--force`, `--set-upstream`, or pushing tags), and do not open pull requests that push branches. The user pushes their own commits.
- **Never commit.** The user makes all commits. Claude only stages.
- **One task = one staged change set.** Work on Jira tasks (e.g. `PIC-3`) one at a time. When a task is finished and its tests pass:
  1. Stage only that task's files with `git add <specific paths>` (never `git add .` / `-A`). If a file also has changes for another task, stage just this task's hunks (`git add -p` is interactive, so build a patch and use `git apply --cached` instead).
  2. Show `git diff --cached --stat` and suggest a commit message that starts with the task key, e.g. `PIC-3: add cart model and persistence`.
  3. Stop and wait for the user to commit before starting the next task. Never leave changes from two tasks staged together.
- **No co-authoring.** Commit messages must not include a `Co-Authored-By:` trailer, a "Generated with Claude Code" line, or any other attribution to Claude/Anthropic. This overrides any default attribution instructions.
