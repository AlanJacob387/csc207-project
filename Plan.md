# Plan

**Project:** Team Sports — a desktop platform that keeps a sports team's schedule, attendance, stats and team info in one place (see README).
**Horizon:** one month — Sprint 1 starts Thu 2026-10-29, final delivery Sun 2026-11-29.
**Team size:** 5 people. They are written as **Person 1–5 / Lane L1–L5** below; replace with real names once the team assigns lanes.

## How this plan is organised

- **5 lanes (L1–L5)** — one per person. A lane owns a themed group of use cases plus the packages and pages that go with them, so two people rarely touch the same file.
- **5 sprints** — Sprint 1 builds the foundation, Sprints 2–4 deliver feature slices, Sprint 5 hardens and prepares the presentation. Each sprint has an internal deadline (as required by the TeamContract) and an exit criterion that must be demoable.
- **One feature = one Clean Architecture vertical slice**, copied from the existing `create_event` template: `use_case/<name>/` → `interface_adapter/<name>/` → a view, wired up in `Main` (later `AppBuilder`).
- **TeamContract rule:** work lands on `main` only when it runs and has been reviewed by a teammate who did not write it. Work in progress stays on `feature/<use-case>` branches.

## Lanes and ownership

| Lane | Owner | Theme | Owns (packages / pages) | User stories | Priority |
|------|-------|-------|--------------------------|--------------|----------|
| L1 | Person 1 | Scheduling & events | `create_event` (existing), `view_schedule`, `export_schedule`, event details/edit/cancel, `CalendarView`, `IcsExporter`, `AppBuilder` | 1, 3 (view half), 5 (trigger) | P0 |
| L2 | Person 2 | Attendance & stats | `record_attendance`, `view_stats`, `AttendanceRecord`, stats entities, "My Stats" page | 2, 4 | P0 / P1 |
| L3 | Person 3 | Accounts & auth | `login`, `signup`, `manage_users`, `DBDataAccessObject`, `AppShell` role filtering, build file | 8 | P0 (login) / P2 (admin) |
| L4 | Person 4 | Teams & roster | `manage_teams`, roster page, parent↔child linking, entity model owner | 7, 6 (link half) | P0 / P1 |
| L5 | Person 5 | Communications | announcements, event-change notifications, absence notices, demo seed data | 5, 6 (notice half) | P1 / P2 |

**Floating capacity:** whoever finishes their lane early takes the top item from the *Unclaimed backlog* at the bottom of this file. Lanes are re-balanced at every sprint review.

## Sprint overview

| Sprint | Dates (inclusive) | Theme | Exit criterion (must be demoable) |
|--------|-------------------|-------|-----------------------------------|
| 1 | Thu 2026-10-29 → Wed 2026-11-04 | Foundation: build, persistence, login, wiring | A clean clone runs, a user logs in against the real DB, sees only their role's pages, and a created event survives a restart |
| 2 | Thu 2026-11-05 → Wed 2026-11-11 | Scheduling, RSVP, attendance | Coach sees real events in the calendar and marks attendance; player RSVPs |
| 3 | Thu 2026-11-12 → Wed 2026-11-18 | Stats, rosters, communications | Player stats page works, admin manages a roster, announcements + notifications appear |
| 4 | Thu 2026-11-19 → Wed 2026-11-25 | Parents, export, weather, polish | Parent flows work, `.ics` export runs as a use case, weather shows on event details |
| 5 | Thu 2026-11-26 → Sun 2026-11-29 | Hardening & presentation | Tests green, docs complete, demo rehearsed, no P0 bugs |

## Sprint 1 — Foundation (Thu 2026-10-29 → Wed 2026-11-04)

Nothing can be built in parallel until persistence, navigation and roles are real, so **all five lanes take one blocker each**. Lanes L1, L2, L4 and L5 are unblocked from day two because they work against the frozen entity contracts plus an in-memory fake.

| ID | Task (breakdown unit) | Owner | Depends on | Deliverable | Due |
|----|----------------------|-------|------------|-------------|-----|
| 1.0 | Add the Gradle build (`build.gradle`, `settings.gradle`) with OkHttp, `org.json`, JUnit; document `gradle run` / `gradle test` in the README | L3 | — | app builds from a clean clone | Fri 10-30 |
| 1.1 | **Decision gate:** freeze the entity model (`User`, `Team`, `Event`, `AttendanceRecord`, plus any new `Announcement` / parent-link / stats entity) — majority vote per TeamContract | All (L4 leads) | — | frozen entities on `main`, recorded in Documentation.md | Sat 10-31 |
| 1.2 | `DBDataAccessObject` against the course DB API (signup, login, JSON blobs for teams/events/RSVPs/attendance) | L3 | 1.0 | gateway + `FakeUserDataAccessObject` for tests | Mon 11-02 |
| 1.3 | `login` + `signup` use cases; replace the TODOs in `LoginView` (login button + "go to signup") | L3 (reviewer: L2) | 1.2 | real login, logged-in role available to the shell | Tue 11-03 |
| 1.4 | `AppBuilder` refactor + `ViewManagerModel` so presenters can trigger navigation | L1 | 1.0 | `Main` slimmed to one `add...UseCase()` per feature | Tue 11-03 |
| 1.5 | Role-based nav filtering in `AppShell` (existing TODO in that file) | L4 | 1.3 | sidebar shows only the pages the role may use | Wed 11-04 |
| 1.6 | Persistence adapters for events and teams; stop wiring `InMemoryEventDataAccessObject` in `Main` | L5 | 1.2 | a created event survives an app restart | Wed 11-04 |
| 1.7 | `Documentation.md` skeleton: layer diagram, package map, glossary | L5 | 1.1 | diagram + package map committed | Wed 11-04 |
| 1.8 | Test harness: JUnit in the build + unit tests for the existing `create_event` slice | L2 | 1.0 | `gradle test` green locally | Wed 11-04 |

**Sprint 1 review (Wed 11-04):** demo the exit criterion, re-balance lanes, and hand any slipped task to the person with slack. Sprints 2–4 assume this sprint landed.

## Sprint 2 — Scheduling, RSVP and attendance (Thu 2026-11-05 → Wed 2026-11-11)

| ID | Task | Owner | Depends on | Deliverable | Due |
|----|------|-------|------------|-------------|-----|
| 2.1 | `view_schedule` use case: `CalendarView` reads real events; delete `addDemoEvents()` | L1 | 1.4, 1.6 | month/week views show stored events | Mon 11-09 |
| 2.2 | "Events" list page (replace the placeholder in `Main`), with filters by type/team | L1 | 2.1 | working list page | Tue 11-10 |
| 2.3 | Event details page + `edit_event` / `cancel_event` use cases (the date-click TODO in `CalendarView`) | L1 | 2.1 | cancelled events visibly flagged everywhere | Wed 11-11 |
| 2.4 | `record_attendance` use case (PRESENT / ABSENT / EXCUSED per player) | L2 | 1.5, 1.6 | coach marks a whole roster's attendance | Tue 11-10 |
| 2.5 | `rsvp` use case: player responds to an upcoming event | L2 | 2.1 | RSVP stored and visible to the coach | Wed 11-11 |
| 2.6 | `manage_teams` use case: create team, assign coach, add/remove players (Roster page) | L4 | 1.5 | Roster page functional | Wed 11-11 |
| 2.7 | `Announcement` entity + `post_announcement` use case | L5 | 1.1 | Announcements page lists posts | Tue 11-10 |
| 2.8 | Event-change notification feed (created / changed / cancelled) | L5 | 2.3, 2.7 | players and parents see a notification feed | Wed 11-11 |
| 2.9 | Sprint 2 test verification + coverage check (non-authors only) | L3 | 2.1–2.8 | green suite, gaps logged as issues | Wed 11-11 |

**Sprint 2 review (Wed 11-11):** demo calendar → attendance → RSVP end-to-end on the seed data. Each slice author writes their own unit tests (part of the Definition of Done); task 2.9 is an independent check by someone who did not write those slices.

## Sprint 3 — Stats, rosters and communications (Thu 2026-11-12 → Wed 2026-11-18)

| ID | Task | Owner | Depends on | Deliverable | Due |
|----|------|-------|------------|-------------|-----|
| 3.1 | `view_stats` use case + "My Stats" page (attendance history + performance) | L2 | 2.4 | player sees their own history and progress | Mon 11-16 |
| 3.2 | Implement the stats decision from 1.1 (structured per-sport stats if voted in) | L2 | 1.1, 3.1 | stats captured and displayed | Tue 11-17 |
| 3.3 | Parent↔child linking (invite/accept flow) + parent view of a child's schedule | L4 | 2.6, 1.3 | parent sees their child's events | Tue 11-17 |
| 3.4 | `submit_absence_notice` use case: parent submits, coach sees it | L5 | 3.3 | coach sees absence notices | Wed 11-18 |
| 3.5 | `manage_users` use case: admin creates/deactivates accounts and changes roles | L3 | 1.3 | admin page works and roles are enforced | Wed 11-18 |
| 3.6 | `export_schedule` use case: promote `IcsExporter` behind a data-access gateway | L1 | 2.1 | export runs from the calendar UI | Wed 11-18 |
| 3.7 | Weather on event details: implement `OpenMeteoWeatherGateway` + controller/presenter | L1 | 2.3 | forecast + rain-risk flag on an event | Wed 11-18 |
| 3.8 | `Documentation.md`: sequence diagrams for login, create-event and attendance | L4 | 1.3, 2.4 | diagrams committed | Wed 11-18 |
| 3.9 | Sprint 3 test verification (non-authors only) | L5 | 3.1–3.7 | green suite | Wed 11-18 |

## Sprint 4 — Integration, polish and demo data (Thu 2026-11-19 → Wed 2026-11-25)

| ID | Task | Owner | Depends on | Deliverable | Due |
|----|------|-------|------------|-------------|-----|
| 4.1 | Full-app bug bash — each lane tests a lane that is not theirs | All | 3.x | triaged bug list | Sun 11-22 |
| 4.2 | UI consistency pass: navigation, empty states, error messages | L4 | 4.1 | consistent UI across all pages | Mon 11-23 |
| 4.3 | Robustness/validation review across slices (bad input, missing data, password handling) | L3 | 4.1 | validation issues closed | Mon 11-23 |
| 4.4 | Demo seed data: 1 admin, 2 coaches, 8 players, 4 parents, 3 teams, 2 weeks of events | L5 | 4.1 | one-click seed for the presentation | Tue 11-24 |
| 4.5 | `Documentation.md` final pass: class diagram, CRC cards, API descriptions | L2 | 4.1 | docs complete | Tue 11-24 |
| 4.6 | README: screenshots/GIFs, API section, user-story ownership table, run instructions | L1 | 4.1 | README current | Tue 11-24 |
| 4.7 | Demo dry run #1 with timings | All | 4.4 | script + timing notes | Wed 11-25 |

## Sprint 5 — Hardening and presentation (Thu 2026-11-26 → Sun 2026-11-29)

| ID | Task | Owner | Depends on | Deliverable | Due |
|----|------|-------|------------|-------------|-----|
| 5.1 | Fix P0/P1 bugs found in the dry run | All | 4.7 | bug list empty | Fri 11-27 |
| 5.2 | Final `main` verification: clean clone → build → run → test | L1 | 5.1 | verification note in the README | Sat 11-28 |
| 5.3 | Merge freeze, demo rehearsal #2, peer-evaluation prep | All | 5.2 | frozen `main`, rehearsed demo | Sun 11-29 |

## How to split work without stepping on each other

1. **Freeze shared contracts before parallel work.** Task 1.1 locks the entities and every `...DataAccessInterface`. After that, changing a shared entity or interface needs sign-off from two lanes, because everyone else depends on it.
2. **One slice, one owner, one package.** A lane never edits another lane's `use_case/<name>` or `interface_adapter/<name>` package; open a PR or send an interface-change request instead.
3. **One page owner per shared file.** `Main.java` (later `AppBuilder`) and `AppShell` are the most conflict-prone files: L1 owns `AppBuilder`, L4 owns `AppShell`, everyone else asks them to add a page or button.
4. **Interfaces first, then UI.** Each slice lands as two commits — use case + tests, then the view — so a reviewer can test the slice without the UI.
5. **Branches:** `feature/<use-case>` (e.g. `feature/record_attendance`). PRs are reviewed by a non-author and `main` must stay runnable (TeamContract).
6. **Daily async check-in** in the group chat — done / doing / blocked — and blockers are raised within 24 h (TeamContract).

## Definition of Done (every slice)

- `use_case/<name>/`: InputBoundary, InputData, OutputData, DataAccessInterface, OutputBoundary, Interactor
- `interface_adapter/<name>/`: Controller, Presenter, ViewModel
- View wired in (via `AppBuilder`) with no leftover TODOs in that package
- JUnit tests for the interactor: success path plus at least two failure paths
- Type annotations and manually typed comments, as the TeamContract requires
- Reviewed and approved by a teammate who did not write it

## Critical path and dependencies

- `1.0 build file` → `1.2 DB gateway` → `1.3 login/role` → everything role-gated (`1.5`, `3.5`)
- `1.1 frozen entities` → every slice in Sprints 2–4
- `1.4 AppBuilder / ViewManagerModel` → every new page
- `2.1 view_schedule` → `2.2`, `2.3`, `2.5`, `3.6`, `3.7`
- `2.6 manage_teams` → `3.3` → `3.4`

**If Sprint 1 slips, Sprints 2–4 slip.** Mitigation: only `1.0`, `1.1` and `1.2` are hard blockers — L1, L2, L4 and L5 can start building slices against the frozen entities and an in-memory fake while L3 finishes the DB gateway.

## Risks

| Risk | Impact | Mitigation |
|------|--------|-----------|
| No build file exists yet, so nothing can be run or tested | Everything blocks | Task 1.0 lands on day one (Fri 10-30) |
| Course DB API quirks or downtime | Login slips and blocks all role work | `FakeUserDataAccessObject` lets other lanes proceed |
| Entity churn after Sprint 1 | Simultaneous rework in several lanes | Decision gate 1.1, then two-lane sign-off |
| Merge conflicts in `Main.java` / `AppShell` | Weekly friction and lost work | AppBuilder refactor (1.4) plus one page owner per file |
| External API unavailable during the demo | Demo failure | Cache the last response and show a graceful "unavailable" state |
| ~2 slices per person per sprint is optimistic | Slipped stories | Priority order below — cut from P2 first |

## Priority if time runs short

1. **P0:** Story 1 (create/schedule events), Story 3 (view schedule + RSVP), Story 2 (attendance), Story 7 (admin creates teams)
2. **P1:** Story 4 (player stats), Story 6 (parent view + absence notices), login/roles
3. **P2:** Story 5 (notifications), Story 8 (manage user accounts), weather, `.ics` export

## Open decisions (vote by Sat 2026-10-31, task 1.1)

| # | Decision | Options | Owner |
|---|----------|---------|-------|
| D1 | Stats model | free-text `statsNotes` (current) vs structured per-sport stats | L2 |
| D2 | Announcements | standalone `Announcement` posts vs event-change notifications only | L5 |
| D3 | Parent↔child link | invite code, admin-assigned, or parent enters the player's username | L4 |
| D4 | Children per parent | one child vs many | L4 |
| D5 | Roles per user | one role (`User.role`, current) vs multi-role | L3 |
| D6 | Persistence | course DB API only vs DB plus local JSON fallback | L3 |
| D7 | Graded external API | Open-Meteo (the only gateway stub today) vs another from the README list | L1 |

## Unclaimed backlog (pull when a lane finishes early)

- Full signup page (currently only a TODO in `LoginView`)
- Event search / filter by date range
- Per-team attendance summary (coach dashboard)
- Reminder for events happening in the next 48 hours
- Theme/dark-mode pass on `AppShell`
- Accessibility pass (keyboard navigation, labels)
- README API section refresh
- Optional second external API from the README list (e.g. Nager.Date holiday warnings)

## User-story ownership

| # | User story | Lane | Owner | Sprint |
|---|-----------|------|-------|--------|
| 1 | Coach creates/schedules practices and games | L1 | Person 1 | done (slice exists) |
| 2 | Coach records attendance and performance stats | L2 | Person 2 | 2 |
| 3 | Player views schedule and RSVPs | L1 + L2 | Person 1 / Person 2 | 2 |
| 4 | Player views personal stats and attendance history | L2 | Person 2 | 3 |
| 5 | Parent notified when events are added/changed/cancelled | L5 | Person 5 | 2 |
| 6 | Parent views child's schedule and submits absence notices | L4 + L5 | Person 4 / Person 5 | 3 |
| 7 | Admin creates teams and assigns coaches/players | L4 | Person 4 | 2 |
| 8 | Admin manages user accounts and roles | L3 | Person 3 | 3 |
