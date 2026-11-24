# Documentation

How the project is built. Diagrams marked **owed** are the deliverables named in `Plan.md` (task 1.7).

## 1. What the app is

Team Sports is a desktop application (Java 17 + Swing) that keeps a sports team schedule, attendance, stats and team information in one place. Four roles share one interface, and each role sees only the pages it may use.

## 2. Architecture

Clean Architecture, one package per layer:

| Layer | Package | Holds | Depends on |
|---|---|---|---|
| Entity | `entity` | `User`, `Role`, `Team`, `Event`, `AttendanceRecord`, `Announcement`, `UserFactory`, `PasswordHasher` | nothing |
| Use case | `use_case/<name>` | input/output boundaries and data, the interactor, and the slice data-access interface | entity |
| Interface adapter | `interface_adapter/<name>` | controller, presenter, view model | use_case, entity |
| Interface adapter (shared) | `interface_adapter` | `ViewManagerModel` (which page is showing), `Session` (who is signed in) | the `CurrentUserInterface` port |
| Data access | `data_access` | `AppDataStore` (the interface the wiring holds), `SupabaseAppDataAccessObject` (hosted Postgres), `FileAppDataAccessObject` (offline fallback), `DatabaseConfig` (where the connection comes from), `EntityJsonMapper`, `InMemoryUserDataAccessObject`, `OpenMeteoWeatherGateway` | entity, use-case interfaces |
| View | `view` | `LoginView`, `SignupView`, `DatabaseSettingsView`, `ChangePasswordView`, `AppShell`, `CalendarView`, `CreateEventView`, `AnnouncementsView`, `ViewManager`, `BackgroundImage` | interface_adapter |
| App | `app` | `AppBuilder` (builds the engine), `Main` (states what the app has) | everything |
| Database | `db` | `schema.sql` (tables, constraints, row level security), `README.md` (setup), `probe.ps1` (connection check) | nothing |

- **Layer and package diagram** - owed by L1 (task 1.7)
- **Entity model diagram and CRC cards** - owed by L4
- **Class diagram for attendance/stats** - owed by L2

## 3. Use cases

Every interaction a user can trigger is its own slice. The full catalogue (33 use cases with lane, sprint, priority and status) lives in `Plan.md`.

Built so far:

| Use case | Slice | View | Tests |
|---|---|---|---|
| `login` (UC-A1) | `use_case/login` + `interface_adapter/login` | `LoginView` | `LoginInteractorTest` (11), `LoginViewTest` (2) |
| `signup` (UC-A2) | `use_case/signup` + `interface_adapter/signup` | `SignupView` | `SignupInteractorTest` (8) |
| `logout` (UC-A3) | `use_case/logout` + `interface_adapter/logout` | Log out button in `AppShell` | `LogoutInteractorTest` (2) |
| `change_password` (UC-A4) | `use_case/change_password` + `interface_adapter/change_password` | `ChangePasswordView` (Account page) | `ChangePasswordInteractorTest` (8), `ChangePasswordViewTest` (3) |
| `create_event` (UC-B1) | `use_case/create_event` + `interface_adapter/create_event` | `CreateEventView` | owed by L2 (task 1.8) |
| `manage_users` (UC-A5, UC-A6, UC-A7) | `use_case/manage_users` + `interface_adapter/manage_users` | `ManageUsersView` (Manage Users page, administrators only) | `CreateUserInteractorTest` (11), `ChangeUserRoleInteractorTest` (9), `DeactivateUserInteractorTest` (8), `ReactivateUserInteractorTest` (6), `ListAccountsInteractorTest` (5), `AdminPolicyTest` (11), `ManageUsersViewTest` (14), `ManageUsersFlowTest` (10) |
| `post_announcement` (UC-E1) | `use_case/post_announcement` + `interface_adapter/post_announcement` | Announcements page | `PostAnnouncementInteractorTest` (17) |
| `view_announcements` (UC-E2) | `use_case/view_announcements` + `interface_adapter/view_announcements` | Announcements page | `ViewAnnouncementsInteractorTest` (17) |

- **Sequence diagrams** - owed by each lane for its own use cases (task 3.8, coordinated by L5)

The account slices are the template the others copy: the view calls a controller, the controller builds input data, the interactor works through a narrow data-access interface and reports to a presenter, and the presenter updates a view model or asks the `ViewManagerModel` to change pages.

## 4. Storage

App data lives in a hosted **Supabase Postgres** database, read and written over
its REST API (PostgREST). `SupabaseAppDataAccessObject` uses one table per entity:
a read filters on the key (`?username=eq.coach1`), and a save posts to
`?on_conflict=<key>` with `Prefer: resolution=merge-duplicates`, so one save
touches one row. Because the tables are shared, two clients can no longer
overwrite each other's unrelated records - something the single-document file
store could not avoid.

`db/schema.sql` holds the schema: five tables whose camelCase quoted columns match
the JSON keys `EntityJsonMapper` already produces, check constraints on role and
status, foreign keys between the tables, row level security, and select/insert/
update policies for the `anon` role. `EntityJsonMapper` stays the single place the
record shape lives, so the database and the file store share one format.

**Connecting.** `DatabaseConfig` looks for the project URL and publishable key in
this order: `TEAMSPORTS_DB_URL` / `TEAMSPORTS_DB_KEY`, then the
`teamsports.db.url` / `teamsports.db.key` system properties, then
`%USERPROFILE%\.team-sports\database.properties`, which the in-app **Database
settings** page writes. With nothing configured the app uses the local JSON file
`data/app-data.json` (git-ignored), so a clean clone runs with no account and the
tests stay offline. `AppDataStore` is the interface the wiring holds, so the two
stores are interchangeable and no use case changes.

**When the store is not usable.** The connection is chosen once, when the app
starts. If the configured database cannot be reached, the app still opens on the
sign-in page and shows `Could not reach the account store: …` there; it never
switches to the local file, because two people who believe they share data must
not diverge. A local file that cannot be parsed is moved to
`app-data.json.corrupt-<timestamp>`, the store starts empty and the sign-in page
names the kept file - the old records stay in that file and are not merged back.
`AppBuilderTest`, `FileAppDataAccessObjectTest` and
`FileAnnouncementPersistenceTest` cover both paths against temporary files.

The signed-in user is deliberately not stored: it lives in `Session`, behind the
`use_case/current_user/CurrentUserInterface` port, so the interactors own the
session transitions rather than the views.

**Security note.** Passwords are stored as PBKDF2-HMAC-SHA256 hashes
(`entity/PasswordHasher`: 120 000 iterations, per-user salt, `pbkdf2$…` format).
A credential written before hashing existed is still accepted, so an older store
keeps working, and that fallback is the only place a plaintext value is compared.
The publishable key is meant to ship inside a client and reaches only what row
level security allows; a **secret** key bypasses row level security, so the app
refuses one. Anyone who extracts the publishable key inherits the app's own
permissions, which is why passwords are hashed rather than stored plainly.
`PasswordHasherTest` pins the iteration count, the salt length and the derived key
length, so a weaker hash cannot pass unnoticed.

## 5. How to build, test and run

Needs JDK 17 or newer; `JAVA_HOME` must point at it, or Gradle refuses to start
("requires JVM 17 or later").

```bash
./gradlew build            # compile
./gradlew test             # unit tests (network-tagged tests skipped, so this works offline)
./gradlew test -Pnetwork   # also runs the live checks against Open-Meteo and the database
./gradlew run              # start the app
```

Demo accounts are created on first run (password `pw`): `coach1`, `player1`,
`parent1`, `admin1`. Their passwords are hashed as they are written.

To use the shared database, start the app, open **Database settings** on the
sign-in page, paste the project URL and publishable key, test and save
(`db/README.md` has the project setup). Without that, the app runs on
`data/app-data.json`. The sign-in page shows which store is active.

## 6. Known limitations

- **One shared credential, no per-user isolation.** Every client uses the same
  publishable key and therefore the `anon` role, so row level security is
  table-wide rather than per account. Real per-user isolation needs Supabase Auth
  with policies keyed on `auth.uid()`, which also replaces our own login backend.
- **Last write wins.** Two people editing the same row at the same time overwrite
  each other; there is no versioning or optimistic locking. Different rows are
  independent.
- **A free project pauses after 7 days without activity.** The app then opens on
  the sign-in page and shows `Could not reach the account store: …` there; it does
  not switch to a local file, because two people who believe they share data must
  not diverge. Resume the project from the dashboard and signing in works again.
  Every remote call runs before the page can answer, so a paused project is felt as
  a wait of up to 15 seconds and then a message, not as a greyed-out button.
- **A damaged local file is set aside, not repaired.** `data/app-data.json` that
  cannot be parsed is moved to `app-data.json.corrupt-<timestamp>`, the store starts
  empty and the sign-in page names the kept file. Nothing is merged back: the old
  records are in that file. A file that cannot be *read* at all (permissions) still
  stops startup, because there is no store to open.
- **An administrator's rights are re-checked against the stored account, not the
  session.** `AdminPolicy` re-reads the signed-in account on every Manage Users
  action, so a demoted or deactivated administrator loses those actions immediately,
  even in an open session. Two gaps remain: the sidebar is rebuilt only when the
  session changes, and a *promoted* account needs a fresh sign-in before its new
  role does anything. Outside Manage Users nothing re-checks the account, so a
  deactivated user keeps working until they log out.
- **The last active administrator is a backstop, not a lock.** An administrator
  cannot act on their own account, so the "one active administrator left" check is
  only reachable when two administrators act at the same moment; the read and the
  write are separate requests, so two simultaneous demotions can still leave none.
- **No delete policy** on any table: accounts are deactivated rather than deleted,
  and demo data is reset from the Supabase SQL editor.
- The calendar still shows hard-coded demo events until `view_schedule` lands
  (task 2.1).
- **The administrator page's role check runs in the app, not in Postgres.** Every
  client connects as the same `anon` role, so the database cannot tell an
  administrator from a player. `AdminPolicy` refuses a non-administrator inside
  each use case and the sidebar only hides the page, so the guard holds in
  business logic, but a client holding the key could still read the accounts
  table through REST directly.
- **Remote calls run on the Swing event thread**, like every other page. With a
  slow project or a paused one the window waits for the request (5 s to connect,
  10 s to read) instead of showing a progress state; a background worker for the
  store is the fix we would make next.
- **Creating an account validates the username and then upserts.** Two
  administrators submitting the same username in the same moment would have the
  second write replace the first account's credential, because `save` is an
  upsert. The window is milliseconds and the username is checked first; an
  insert-only path for account creation belongs to the 4.3 pass.
- **Password rules and sign-up are still loose.** Any non-empty password is
  accepted and sign-up admits `ADMIN`, so the administrator page is not the only
  route to an administrative account. Nothing caps a username or a display name
  outside the administrator page (40 and 60 characters there), so a very long name
  can be created through sign-up and is then clipped in the account list. One shared
  password policy, one length rule and a restricted sign-up belong to the 4.3
  validation pass.
- One role per user (decision D5).

## 7. API notes

Open-Meteo (no key required) - the weather channel. Called successfully from Java
on 2026-10-29 and documented in the README API section in the course required
format (link, how it is used, whether it was called). Alternatives we considered
are listed in `Plan.md` decision D7.

Supabase (Postgres + REST / PostgREST) - the shared store for accounts, teams,
events, attendance and announcements. Its routes were first verified against the
real project on 2026-11-23: all five tables answered `200 []`, and `/rest/v1/`
answered `401 Secret API key required`, which is the healthy answer there and
proves PostgREST is reachable. Decision D6 in `Plan.md`, schema in
`db/schema.sql`, setup and troubleshooting in `db/README.md`.