# Lane L3 — Accounts & Auth (Andy)

**Lane:** L3 · **Owner:** Andy · **Sprint 1 window:** Thu 2026-10-29 → Wed 2026-11-04
**Source of task IDs:** `Plan.md`. This file is the detail behind L3's rows there; if the two disagree, `Plan.md` wins and this file gets updated.

## Scope

| Plan task | What it is | Status |
|---|---|---|
| 1.0 | Gradle build file | ✅ done (commit `223f630`, Gradle 9.8.2 + OkHttp + org.json + JUnit 5) |
| 1.2 | Storage gateway for accounts | done 2026-10-29 |
| 1.3 | `login` + `signup` use cases | done 2026-10-29 |
| 1.4 | `AppBuilder` refactor + `ViewManagerModel` (taken over from L1) | done 2026-10-29 |
| 1.5 | Role-based navigation in `AppShell` | done 2026-10-29 |
| 2.10 | `logout` use case | done 2026-11-02 |
| 3.10 | `change_password` use case | done 2026-11-02 |
| 3.5 | `manage_users` (admin) | done 2026-11-23: create, change role, deactivate and reactivate, plus the read that fills the page, behind one administrators-only page |
| 4.8 | Hosted database store (revision of D6): Supabase Postgres, schema, in-app connection settings | done 2026-11-23 |
| 4.3 | Validation / robustness pass | Sprint 4 |
| — | Week-1 API items (spike + README API section) | done 2026-10-29 |

## Decisions locked

| # | Decision | Choice | Why |
|---|----------|--------|-----|
| D6 | Persistence | **Revised 2026-11-23: hosted Supabase Postgres** over its REST API (PostgREST), one table per entity, written with `?on_conflict=` upserts | Data is now shared between devices instead of living on one laptop, and two clients no longer overwrite each other's unrelated records the way a whole-document file did. The local JSON file stays as the offline default when nothing is configured, so a clean clone and the tests need no account. Connection settings are saved per user by the in-app **Database settings** page, so no key enters git and nobody needs a terminal |
| — | Role at signup | Dropdown on the signup form, defaults to `PLAYER` | Lets all four roles be demoed without admin tooling |
| — | Passwords | **PBKDF2-HMAC-SHA256 hashes** (`entity/PasswordHasher`): 120 000 iterations, a per-user salt, iteration count stored with the hash; a value written before hashing is still accepted so an older store keeps working | Once the data became shared, a plaintext store plus the publishable key would have exposed every password. Hashing arrived with the hosted store (2026-11-23) and is recorded in `Documentation.md` §4 |
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
| `use_case/change_password/ChangePasswordUserDataAccessInterface` | `User get(String username)`, `void save(User user)` |
| `use_case/current_user/CurrentUserInterface` | `getCurrentUsername()`, `setCurrentUser(User)`, `clearCurrentUser()`. Implemented by `interface_adapter.Session`, so the account use cases own the session transitions instead of the view wiring |
| ~~`data_access/DBDataAccessObject`~~ | **Deleted 2026-10-29** — it targeted the retired 2025F course API and could not be verified. A hosted store can be added later behind the same interfaces |
| `data_access/AppDataStore` | The union of the per-use-case interfaces plus the app-wide reads, held by `AppBuilder`, so the file store and the database are interchangeable and no interactor changes |
| `data_access/SupabaseAppDataAccessObject` | Hosted store: one table per entity; a read filters on the key (`?username=eq.coach1`), a save posts to `?on_conflict=<key>` with `Prefer: resolution=merge-duplicates`, and an announcement is inserted strictly so a duplicate id answers 409 instead of replacing a post |
| `data_access/DatabaseConfig` | Where the connection comes from: `TEAMSPORTS_DB_URL`/`TEAMSPORTS_DB_KEY`, then `-Dteamsports.db.*`, then `%USERPROFILE%\.team-sports\database.properties` (written by the in-app settings page). Refuses a secret key, which would bypass row level security |
| `data_access/FileAppDataAccessObject` | Local JSON store, used when nothing is configured: the offline default for a clean clone and for the tests. A file it cannot parse is set aside as `app-data.json.corrupt-<timestamp>` and the store starts empty, so a damaged file cannot stop the app; a file that cannot be read at all still throws |
| `data_access/InMemoryUserDataAccessObject` | Test fake for accounts (no network, no disk). App data is tested against a temporary file via `@TempDir`, and the hosted store against a local stub server |
| `data_access/EntityJsonMapper` | Entity ⇄ JSON in **one** place, shared by the file store and the database so the record shape cannot drift |

**On-disk shape** (`data/app-data.json`, git-ignored; created on first write):

```json
{
  "users": [{"username": "coach1", "password": "pw", "displayName": "Coach One", "role": "COACH"}],
  "teams": [{"teamId": "t1", "name": "Raptors", "sport": "Basketball", "coachUsername": "coach1", "playerUsernames": ["p1"]}],
  "events": [{"eventId": "e1", "teamId": "t1", "type": "GAME", "dateTime": "2026-11-01T18:00", "location": "Gym A", "opponent": "Bears", "cancelled": false}],
  "attendance": [{"eventId": "e1", "playerUsername": "p1", "status": "PRESENT", "statsNotes": "2 goals"}]
}
```

Note: passwords are stored as PBKDF2 hashes (`entity/PasswordHasher`), not plaintext; a value written before hashing existed is still accepted, so an older file keeps working. The file is git-ignored - do not commit real credentials. The database tables in `db/schema.sql` mirror this shape exactly (camelCase quoted columns), so the same `EntityJsonMapper` serves both stores.

**Hosted-store swap (done 2026-11-23):** `SupabaseAppDataAccessObject` implements the same interfaces and reuses `EntityJsonMapper` unchanged; `AppBuilder.createStore` picks it when settings exist and keeps the file otherwise. The use cases, their tests and the in-memory fakes did not change - only the wiring did, which is what this design was for.

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
| The hosted project pauses after 7 days idle, or there is no network | Resume it from the Supabase dashboard (data is kept for a year); the app reports the failure instead of silently using a local file; `db/probe.ps1` names the cause; with no settings at all the file store still runs |
| The local JSON file is deleted or corrupted | `load()` treats a missing file as an empty store; keep the demo seed script (task 4.4) able to recreate data |

## Change log

| Date | Change |
|------|--------|
| 2026-11-24 | Merged the new `main` (scheduling, parent links, absence notices, notifications, weather, presentation seed) into this branch; PRs #15 and #16 had conflicts. **Hosted store:** `AppDataStore` now extends every use-case port main added, with the questions every store answers the same way (team membership, event lookups, linked players) as default methods, so `SupabaseAppDataAccessObject` only implements what is really stored: three new tables (`parent_links`, `absence_notices`, `event_notifications`, in `db/schema.sql`), a delete policy on `parent_links` only, and `saveEvent` now also writes the change notification through the shared `EventChangeNotifications` (two requests, not one transaction; see §6). Not run against a live project. **File store:** main's save-then-rollback replaces this branch's stage-then-write; both leave memory untouched when a write fails. One policy for an unreadable file: malformed or duplicated communication sections are now set aside like any other unreadable file instead of stopping startup, and main's three tests that asserted the old behaviour (`FileCommunicationsPersistenceTest`) now assert that the bytes are kept in the `.corrupt-` file and nothing is loaded. **Wiring:** `AppBuilder` takes any `AppDataStore`; a local file gets main's presentation dataset, a hosted store only the four demo accounts. `Session.getRole` was already `getCurrentRole` on this branch, so main's two new views and eight test doubles were updated to it. **Cross-lane:** this edits L1 (`AppBuilder`, `Main`), L5 (`AbsenceNoticesView`, `ParentLinksView`, `FileCommunicationsPersistenceTest`) and L1/L5 test doubles; none has a recorded sign-off. 449 tests green (58 classes). |
| 2026-11-23 | Second PR review, from an audit of the whole branch rather than of the last change. **Store:** every saver now writes the document first and records the change in memory only after the write returns, so a disk error leaves the store exactly as it was. Before this, a failed save reported failure and then persisted on the next unrelated write - the "failed writes leave memory changed" finding Daniel raised on 2026-11-09, still open until now (`Lane-L5.md`); `FileAppDataAccessObjectTest.aFailedWriteLeavesEverySectionAsItWas` covers all five sections and fails if the order is swapped back. **Credentials:** a plaintext credential is re-hashed on the next successful sign-in (`LoginInteractor.upgradeLegacyCredential`), so an older store converges instead of keeping a readable password forever; a store that cannot rewrite it still signs the user in, and the login port gained the one `save` it needs for this. **Connection settings:** a legacy `service_role` JWT is refused alongside an `sb_secret_` key, because both bypass row level security, and a URL that is not HTTPS is refused unless it points at this machine, so the key cannot be sent in the clear. **Docs:** §6 now says that row level security grants writes, not just reads, to anyone holding the publishable key; that the demo accounts ship on the shared store with password `pw`; and that one Manage Users action can hold the window for about a minute on a paused project. §4 records the re-hash and both refused key formats. 252 tests green. **Cross-lane, needs sign-off:** this commit changes nothing outside L3, but the branch also edits `view/AnnouncementsView.java` (L5, one line following the `Session.getRole` → `getCurrentRole` rename) and `Lane-L5.md` (L5's review document, annotated with the resolved startup finding); neither was recorded here until now. |
| 2026-11-23 | PR review fixes, all found by an independent audit of this branch. **Startup:** a store that cannot be reached no longer kills the app - `AppBuilder.seedDemoDataIfEmpty` reports the failure instead of throwing, and the sign-in page shows it, so a paused database costs a message rather than a launch; a file that cannot be parsed is now set aside as `app-data.json.corrupt-<timestamp>` and the store starts empty, also instead of aborting (a half-loaded store is cleared, so a later save cannot drop the sections that failed). **Authorization:** `AdminPolicy` re-reads the administrator's stored account on every Manage Users action, so a demoted or deactivated administrator loses those actions in an open session; a store that cannot confirm the caller is a refusal, not a pass. The last-admin rule is now documented and tested as a concurrency backstop, because a verified administrator cannot act on their own account. **Page:** the role picker starts on the selected account's own role and the create form on `PLAYER`, so pressing Change role cannot re-role somebody by accident. **Tests:** `PasswordHasherTest` pins the work factor and the salt/key lengths, and a mutation that set the work factor to 1 used to pass the whole suite. 244 tests green, 74 of them in the `manage_users` slice. **Cross-lane, needs sign-off:** `AppBuilder` (L1) changed in the seed method only, and `FileAnnouncementPersistenceTest` (L5) now asserts the set-aside contract instead of an exception. |
| 2026-11-23 | `manage_users` built (UC-A5 to UC-A7): create an account, change a role, deactivate and reactivate an account, plus the read that fills the page, on one administrators-only page. Authorization and the "last active administrator" rule live in `AdminPolicy`, so they hold even if the page is called directly; a deactivated account is kept for its history, refused at login, and can be restored without touching the database. 59 new tests, 221 green (counts before the review fixes above) |
| 2026-11-23 | Persistence moved to a hosted **Supabase Postgres** database (D6 revised): `db/schema.sql` (five tables, camelCase quoted columns, constraints, row level security), `SupabaseAppDataAccessObject`, `AppDataStore` as the wiring contract, `DatabaseConfig` + an in-app Database settings page so no key enters git and nobody needs a terminal; passwords now stored as PBKDF2 hashes; the local file remains the offline default; 162 tests green |
| 2026-10-29 | File created: scope, decisions, storage design, phases, API items |
| 2026-10-29 | Course API dropped (retired host, not a course requirement); storage is the local JSON file; `DBDataAccessObject` deleted; statuses restored |
| 2026-10-29 | `AppBuilder` created and `Main` reduced to the view/use-case chain; `BackgroundImage` extracted so login and signup share the stadium photo; `SignupView` redesigned (labelled fields, link-style back button) |
| 2026-11-02 | Account use cases reorganised for CA/SOLID: `CurrentUserInterface` port so interactors own the session; `logout` and `change_password` built as full slices (`use_case/` + `interface_adapter/` + Account page); `LoginPresenter` no longer touches the session; `existsByName` removed from the login port (ISP); dead code removed (`InMemoryEventDataAccessObject`, `saveProfile`); 54 tests green and the app launches |
| 2026-11-16 | PR review fixes: the Account page now clears its three fields and any message when the signed-in user changes (`ChangePasswordView` listens to `Session`), so one user's password draft can never be submitted by the next user (`ChangePasswordViewTest`, 3 tests); `gradlew` committed as mode 100755 so `./gradlew` works on a clean Unix checkout; `bin/` ignored; 57 tests green and the app launches |
| 2026-10-29 | Login and signup built end to end: use cases, controllers, presenters, view models, `ViewManagerModel`, `Session`, `SignupView`; `LoginView` wired; `AppShell` filters pages by role; `Main` rewired to the file store; 42 tests green and the app launches |

