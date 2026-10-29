# Lane L3 — Accounts & Auth (Andy)

**Lane:** L3 · **Owner:** Andy · **Sprint 1 window:** Thu 2026-10-29 → Wed 2026-11-04
**Source of task IDs:** `Plan.md`. This file is the detail behind L3's rows there; if the two disagree, `Plan.md` wins and this file gets updated.

## Scope

| Plan task | What it is | Status |
|---|---|---|
| 1.0 | Gradle build file | ✅ done (commit `223f630`, Gradle 9.8.2 + OkHttp + org.json + JUnit 5) |
| 1.2 | Storage gateway for accounts | done 2026-10-29 |
| 1.3 | `login` + `signup` use cases | done 2026-10-29 |
| 1.5 | Role-based navigation in `AppShell` | done 2026-10-29 |
| 3.5 | `manage_users` (admin) | deferred — P2, Sprint 3 |
| 4.3 | Validation / robustness pass | Sprint 4 |
| — | Week-1 API items (spike + README API section) | done 2026-10-29 |

## Decisions locked

| # | Decision | Choice | Why |
|---|----------|--------|-----|
| D6 | Persistence | **Local JSON file only** (`data/app-data.json`): users incl. passwords, roles, teams, events, attendance | No server is needed, and the 2025F course API turned out to be retired (dead host and port, and not a course requirement). A hosted store (Firebase RTDB, JSONBin) is a later one-class swap behind the same interfaces |
| — | Role at signup | Dropdown on the signup form, defaults to `PLAYER` | Lets all four roles be demoed without admin tooling |
| — | Passwords | Plaintext in the local store; to be recorded in `Documentation.md` (task 4.5) | Course scope; hashing adds work with no graded benefit. Revisit before any real deployment |
| — | `manage_users` | Deferred to P2 / Sprint 3 | No API support for listing users; needs the local registry first |
| — | Java toolchain | Pin a Java 21 toolchain **only if** Gradle 9.8.2 fails on JDK 26 | Keeps `options.release = 17` working without forcing everyone's JDK down |

## Role → page map

| Page | ADMIN | COACH | PLAYER | PARENT |
|---|---|---|---|---|
| Calendar | ✅ | ✅ | ✅ | ✅ (child's events) |
| Events (list) | ✅ | ✅ | ✅ | ✅ |
| New Event | — | ✅ | — | — |
| Attendance | — | ✅ | — | — |
| Announcements | ✅ | ✅ | ✅ | ✅ |
| My Stats | — | — | ✅ | — |
| Roster | ✅ | ✅ (view) | — | — |
| Manage Users | ✅ | — | — | — |
| Absence Notices | — | — | — | ✅ |

Pages not listed for a role are hidden in the sidebar (not disabled), so the shell looks clean per role.

## Demo accounts (seeded by `Main` when the store is empty)

| Username | Password | Role | Sidebar pages |
|---|---|---|---|
| coach1 | pw | COACH | Calendar, Events, New Event, Announcements, Roster |
| player1 | pw | PLAYER | Calendar, Events, Announcements, My Stats |
| parent1 | pw | PARENT | Calendar, Events, Announcements |
| admin1 | pw | ADMIN | Calendar, Events, Announcements, Roster |

Created only when the store has no `coach1`, so real data is never overwritten. Replace with the team's seed script (task 4.4).

## Storage design

Follows the course convention: **one narrow data-access interface per use case**, and a single DAO class that implements as many of them as it needs (exactly how the course's `DBUserDataAccessObject` implements three interfaces).

| File | Role |
|---|---|
| `use_case/login/LoginUserDataAccessInterface` | `User get(String username)` |
| `use_case/signup/SignupUserDataAccessInterface` | `boolean existsByName(String username)`, `void save(User user)` |
| ~~`data_access/DBDataAccessObject`~~ | **Deleted 2026-10-29** — it targeted the retired 2025F course API and could not be verified. A hosted store can be added later behind the same interfaces |
| `data_access/FileAppDataAccessObject` | Local JSON store for app data; implements the per-use-case interfaces it needs |
| `data_access/InMemoryUserDataAccessObject` | Test fake for accounts (no network, no disk). App data is tested against a temporary file via `@TempDir` |
| `data_access/EntityJsonMapper` | Entity ⇄ JSON in **one** place, so the on-disk shape is unit-testable and reusable by a future Firebase DAO |

**On-disk shape** (`data/app-data.json`, git-ignored; created on first write):

```json
{
  "users": [{"username": "coach1", "password": "pw", "displayName": "Coach One", "role": "COACH"}],
  "teams": [{"teamId": "t1", "name": "Raptors", "sport": "Basketball", "coachUsername": "coach1", "playerUsernames": ["p1"]}],
  "events": [{"eventId": "e1", "teamId": "t1", "type": "GAME", "dateTime": "2026-11-01T18:00", "location": "Gym A", "opponent": "Bears", "cancelled": false}],
  "attendance": [{"eventId": "e1", "playerUsername": "p1", "status": "PRESENT", "statsNotes": "2 goals"}]
}
```

Note: passwords **are** stored in this file as plaintext (see the decision table above), so the local store can serve login with no server at all. The file is git-ignored - do not commit real credentials.

**Hosted-store swap path** (if the team later wants cross-device state): add e.g. `FirebaseDataAccessObject` or `JsonBinDataAccessObject` implementing the same interfaces, reuse `EntityJsonMapper` unchanged, change one line in the wiring. Unit tests keep using the in-memory fakes and do not change.

## Phases and tasks

### Phase 0 — Build and test harness (item 2)

| ID | Task | Deliverable |
|----|------|-------------|
| L3-0.1 | Run `gradlew build`, `gradlew test`, `gradlew run` on the current JDK (26 locally) | Either a working build, or a documented Java 21 toolchain pin |
| L3-0.2 | Create the missing `src/test/java` tree with one smoke test | `gradlew test` executes and passes |
| L3-0.3 | Add generated/state files to `.gitignore` (`data/app-data.json` except a seeded sample) | Clean `git status` after a run |

### Phase 1 — Storage layer (item 3)

| ID | Task | Deliverable |
|----|------|-------------|
| L3-1.1 | `LoginUserDataAccessInterface`, `SignupUserDataAccessInterface` (one per use case, per course convention) | 2 interfaces |
| L3-1.2 | `EntityJsonMapper`: entity ⇄ JSON for `User`, `Team`, `Event`, `AttendanceRecord` | Mapper + round-trip tests |
| L3-1.3 | `FileAppDataAccessObject` — reads/writes `data/app-data.json`, creates the file if missing | DAO + tests |
| L3-1.4 | `InMemoryUserDataAccessObject` — fake for account tests (app data is tested against a temp file) | Fakes + tests |
| L3-1.5 | ~~`DBDataAccessObject`~~ — dropped: the course API is retired, so accounts come from the local store | n/a |
| L3-1.6 | Unit tests: mapper round-trip, file DAO persistence, unknown-user / wrong-password / duplicate-username paths | Green test suite |

### Phase 2 — Login use case (item 4)

| ID | Task | Deliverable |
|----|------|-------------|
| L3-2.1 | `use_case/login/`: InputBoundary, InputData, OutputData, OutputBoundary, DataAccessInterface, Interactor | 6 files + tests |
| L3-2.2 | `interface_adapter/login/`: Controller, Presenter, ViewModel | 3 files |
| L3-2.3 | `interface_adapter/ViewManagerModel` + session holder for the logged-in user | Navigation works from a presenter |
| L3-2.4 | Wire `LoginView` (replace the TODOs at lines 182-201) + `Main` | Real login end to end |

### Phase 3 — Signup and role-based nav (item 5)

| ID | Task | Deliverable |
|----|------|-------------|
| L3-3.1 | `use_case/signup/` + `interface_adapter/signup/` + `entity/UserFactory` | Slice + tests |
| L3-3.2 | New `view/SignupView` with the role dropdown (defaults to `PLAYER`) | Signup screen |
| L3-3.3 | `AppShell` filters nav items by the session role (table above) | Sidebar differs per role |
| L3-3.4 | Update `Main`/`AppBuilder` so `Create an Account` navigates to the signup page | Full flow |

## Week-1 API items (course requirement)

| ID | Task | Status |
|----|------|--------|
| L3-API-1 | **Experiment A:** `POST /user` with an extra `role` field, then `GET /user` to see whether it persists | dropped 2026-10-29 (course API retired) |
| L3-API-2 | **Experiment B:** probe `GET /data?username=…` (and `/userData`) for a generic store | dropped 2026-10-29 (course API retired) |
| L3-API-3 | **Experiment C:** read the Postman workspace folder list for a store-data request | dropped 2026-10-29 (course API retired) |
| L3-API-4 | Prove one API call works from Java (Open-Meteo via OkHttp) with runnable evidence | done 2026-10-29: real call returned max 21.3 C, rain chance 19% |
| L3-API-5 | Rewrite README `## APIs` in the course format: doc link, how it is used, called successfully (yes/no + date) | done 2026-10-29 |

> The 2025F course API host (`vm003.teach.cs.toronto.edu:20112`) no longer answers (checked 2026-10-29: DNS resolves to 142.1.148.13 but TCP port 20112 is refused), and the Postman workspace no longer has the documented section. The course does not require it, so it is out of scope. `services.gradle.org` *is* reachable, so the Gradle build can run here.

## Definition of Done (per slice)

Same as `Plan.md`: the six `use_case/<name>` files, the three `interface_adapter/<name>` files, a wired view, JUnit tests (success + at least two failure paths), typed annotations and hand-written comments, and approval from a teammate who did not write it.

## Coordination

| Item | Agreement |
|------|-----------|
| `ViewManagerModel` + minimal `AppBuilder` (was Alan's task 1.4) | L3 implements them, because login needs presenter-driven navigation; Alan is told and reviews |
| `AppShell` role filtering (task 1.5) | L3 writes the change, Alan reviews (he owns the file in `Plan.md`) |
| Events / attendance persistence | L1 and L2 wire their slices to the same `FileAppDataAccessObject` so there is one store, not three |
| `Plan.md` D6 | Updated to the hybrid choice by L3 |

## Risks

| Risk | Mitigation |
|------|-----------|
| Gradle 9.8.2 may not support the local JDK 26 | Pin a Java 21 toolchain in `build.gradle` (L3 owns the build file) |
| First `gradlew` run downloads ~130 MB | One-time; caches in `~/.gradle` |
| Data is per machine: a coach's laptop and a player's laptop share nothing | Accepted for now and documented as a known limitation; a hosted store behind the same interfaces fixes it later |
| The local JSON file is deleted or corrupted | `load()` treats a missing file as an empty store; keep the demo seed script (task 4.4) able to recreate data |

## Change log

| Date | Change |
|------|--------|
| 2026-10-29 | File created: scope, decisions, storage design, phases, API items |
| 2026-10-29 | Course API dropped (retired host, not a course requirement); storage is the local JSON file; `DBDataAccessObject` deleted; statuses restored |
| 2026-10-29 | Login and signup built end to end: use cases, controllers, presenters, view models, `ViewManagerModel`, `Session`, `SignupView`; `LoginView` wired; `AppShell` filters pages by role; `Main` rewired to the file store; 42 tests green and the app launches |

