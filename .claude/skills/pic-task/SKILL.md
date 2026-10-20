---
name: pic-task
description: Work one Jira task of the Pickaroo project (keys PIC-n) from start to finish the way this repo does it - own branch, Jira In Progress and assigned to Carlo, implement, test, commit with the task key, Jira Done. Use whenever starting or finishing a PIC task.
---

# One PIC task, start to finish

Rules come from `CLAUDE.md`: never push, one task = one branch = one commit, commit messages start with the key, no co-author or "Generated with" line.

## 1. Start
```bash
.claude/skills/pic-task/scripts/start-task.sh PIC-45 live-driver-map
```
Creates `feature/PIC-45-live-driver-map` from the **current** branch (earlier unmerged tasks stay included). Then, in the background, hand Jira to the `jira-updater` agent (Haiku):

> Move PIC-45 (cloud 4c4c0723-c9cb-4b8d-84eb-7d400bf852f9) to "In Progress" with the comment: "Started on branch `feature/PIC-45-live-driver-map`." If it has no assignee, assign Carlo Ruiz. Touch no other issue. Report one line.

Also pass the parent epic ("move PIC-43 to In Progress, no comment") the first time work starts in it. Assignee is always Carlo Ruiz (accountId `712020:0ccefe25-98c8-453b-a236-8ae84122f3ee`); the agent only fills an empty assignee.

## 2. Implement and test
- Read the task's Jira description for scope and acceptance criteria; the repo plan is `docs/PLAN.md`.
- New backend-shaped feature: use the `mock-endpoint` skill. UI tests: `compose-ui-tests`. Visual check: `emulator-ui-check`.
- Run `android-test-run` (unit always; instrumented when UI, navigation or Android code changed). Done means everything passes, not just the new tests.
- Write a regression test for every bug the emulator exposes; real bugs found this way so far: startup crash from building the container in `Application`'s constructor, navigation called from a background thread, accent-sensitive text assertions.

## 3. Finish
```bash
DRY_RUN=1 .claude/skills/pic-task/scripts/finish-task.sh PIC-45 "add live driver map" <paths...>   # check what is staged
.claude/skills/pic-task/scripts/finish-task.sh PIC-45 "add live driver map" <paths...>
```
- Pass explicit paths (this task's files only). Never `git add .`/`-A`; leave `.idea/` and unrelated files out.
- The script shows recent commits first. **The user sometimes commits from Android Studio while you work**, producing a commit with a different message. If most of the task is already committed, commit what remains and say so; do not rewrite or amend.
- Then hand Jira to the agent: move the task to "Done" with a comment containing the branch, the test counts and what was verified on the emulator.

## 4. Report
Two or three lines: what changed, test counts, anything left or decided for the user. Do not wait for approval to continue to the next task unless something is blocked.
