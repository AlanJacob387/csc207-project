---
name: team-sports-workflow
description: Review and change the CSC207 Team Sports repository using its ownership rules, feature naming, focused commits, and data preservation requirements. Applies to Daniel Markusson's L5 work and shared project contributions.
---

# Team Sports project workflow

## Naming and writing

- Name branches, files, classes, documentation, and pull requests after the
  contributor, feature, or project. Omit assistant and model names, generation
  labels, promotional text, and tool attribution trailers from new project work.
- Write direct, specific comments and documentation about behavior and purpose.
  Do not claim work was written manually, invent reviews, or report checks that
  were not performed. Preserve required licenses and existing upstream notices.
- Keep existing package names and course terminology. Avoid renaming unrelated
  files or rewriting older content merely to make it match a style preference.

## Preserve existing work

- Check the current branch, status, staged changes, and relevant diff before
  editing. Keep the user's uncommitted and staged work intact.
- Work on a descriptive branch. Daniel's current branch is
  `daniel/l5-completion`; later feature branches may use names such as
  `feature/post-announcement` or `daniel/absence-notices`.
- Add a missing file only after confirming the path is unused. Patch existing
  files narrowly. Do not replace files wholesale, reset the working tree, amend
  existing commits, rewrite history, or force-push without explicit instruction.
- Never overwrite existing accounts, teams, credentials, or runtime data with
  demo fixtures. Use temporary directories for persistence checks. The current
  startup seed method has an overwrite risk documented in `Lane-L5.md`; avoid
  running the application against an existing default store during diagnosis.

## Commits and reviews

- Use the existing configured Git identity; do not impersonate another author,
  forge dates, or change global Git settings.
- Make each commit a coherent change. Use a short, ordinary subject describing
  the result, such as `Add announcement validation` or `Show team announcements`.
  Add a body only when the reason or a limitation needs explanation.
- Inspect and stage explicit paths or hunks. Review the staged diff and run
  `git diff --cached --check` before committing. Do not include unrelated files,
  runtime data, credentials, or build outputs.
- Follow the team's slice split: business logic and tests first, then adapters
  and the wired view. Report actual validation in the review description.
- A teammate who did not write the change must review it before it reaches
  `main`. Creating a local commit does not authorize pushing, merging, or
  messaging teammates.

## Ownership and architecture

- Read the relevant parts of `Plan.md`, `TeamContract.md`, and the lane notes.
  Daniel owns L5: announcements, event-change notifications, parent absence
  notices, event weather, rain-risk flags, and README build/test instructions.
  `Lane-L3.md` describes Andy's account work.
- Alan owns `AppBuilder`; Aarav owns `AppShell` and the entity model. Coordinate
  wiring changes with those owners. Shared entity/interface changes require
  sign-off from two lanes under `Plan.md`. Do not claim this happened without
  evidence or silently expand scope into another lane's packages.
- Use `login` and `signup` as the reference slices: six files under
  `use_case/<name>`, three adapters under `interface_adapter/<name>`, and a view
  wired through `AppBuilder`. Interactors depend on narrow interfaces.
- Keep role and team authorization in business logic as well as navigation.
  Parent-facing data must respect the parent-to-player link.
- For weather, define a gateway boundary owned by the use case; keep external
  response objects out of the use-case API. Run network work off the Swing event
  thread and represent unavailable data explicitly.

## Validation and costs

- The reviewed baseline uses JDK 21 and targets Java 17 bytecode. The wrapper
  is executable, so use `./gradlew build` and `./gradlew test` on macOS/Linux.
  An explicit `bash ./gradlew` invocation also works.
  Add `--offline` when dependencies are already cached and no downloads are
  intended. On Windows, use `gradlew.bat`.
- The default test suite excludes network-tagged tests. Only run
  `bash ./gradlew test -Pnetwork` when a live API check is part of the task.
  For each implemented slice, cover success and at least two relevant failure
  paths, as required by the team's definition of done.
- Before any potentially billable action or batch, notify the user of who pays,
  the estimated amount/range and currency (or unknown cost), usage assumptions,
  recurring charges, and cleanup implications. Include the same notice in plans
  involving charges and renew it when scope changes. This is a notification
  requirement; continue within existing authorization after disclosure.
