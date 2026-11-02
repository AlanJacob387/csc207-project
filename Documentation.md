# Documentation

How the project is built. Diagrams marked **owed** are the deliverables named in `Plan.md` (task 1.7).

## 1. What the app is

Team Sports is a desktop application (Java 17 + Swing) that keeps a sports team schedule, attendance, stats and team information in one place. Four roles share one interface, and each role sees only the pages it may use.

## 2. Architecture

Clean Architecture, one package per layer:

| Layer | Package | Holds | Depends on |
|---|---|---|---|
| Entity | `entity` | `User`, `Role`, `Team`, `Event`, `AttendanceRecord`, `UserFactory` | nothing |
| Use case | `use_case/<name>` | input/output boundaries and data, the interactor, and the slice data-access interface | entity |
| Interface adapter | `interface_adapter/<name>` | controller, presenter, view model | use_case, entity |
| Interface adapter (shared) | `interface_adapter` | `ViewManagerModel` (which page is showing), `Session` (who is signed in) | the `CurrentUserInterface` port |
| Data access | `data_access` | `FileAppDataAccessObject`, `EntityJsonMapper`, `InMemoryUserDataAccessObject`, `OpenMeteoWeatherGateway` | entity, use-case interfaces |
| View | `view` | `LoginView`, `SignupView`, `ChangePasswordView`, `AppShell`, `CalendarView`, `CreateEventView`, `ViewManager`, `BackgroundImage` | interface_adapter |
| App | `app` | `AppBuilder` (builds the engine), `Main` (states what the app has) | everything |

- **Layer and package diagram** - owed by L1 (task 1.7)
- **Entity model diagram and CRC cards** - owed by L4
- **Class diagram for attendance/stats** - owed by L2

## 3. Use cases

Every interaction a user can trigger is its own slice. The full catalogue (33 use cases with lane, sprint, priority and status) lives in `Plan.md`.

Built so far:

| Use case | Slice | View | Tests |
|---|---|---|---|
| `login` (UC-A1) | `use_case/login` + `interface_adapter/login` | `LoginView` | `LoginInteractorTest` (8) |
| `signup` (UC-A2) | `use_case/signup` + `interface_adapter/signup` | `SignupView` | `SignupInteractorTest` (8) |
| `logout` (UC-A3) | `use_case/logout` + `interface_adapter/logout` | Log out button in `AppShell` | `LogoutInteractorTest` (2) |
| `change_password` (UC-A4) | `use_case/change_password` + `interface_adapter/change_password` | `ChangePasswordView` (Account page) | `ChangePasswordInteractorTest` (8) |
| `create_event` (UC-B1) | `use_case/create_event` + `interface_adapter/create_event` | `CreateEventView` | owed by L2 (task 1.8) |

- **Sequence diagrams** - owed by each lane for its own use cases (task 3.8, coordinated by L5)

The account slices are the template the others copy: the view calls a controller, the controller builds input data, the interactor works through a narrow data-access interface and reports to a presenter, and the presenter updates a view model or asks the `ViewManagerModel` to change pages.

## 4. Storage

One local JSON file, `data/app-data.json` (git-ignored, created on first write), holds users, teams, events and attendance. `EntityJsonMapper` is the single place the on-disk shape lives, so a hosted store can replace the file without touching any use case.

The signed-in user is deliberately not in the file: it lives in `Session`, behind the `use_case/current_user/CurrentUserInterface` port, so the interactors own the session transitions rather than the views.

**Security note:** passwords are stored in plaintext, matching course scope. This must be revisited before any real deployment.

## 5. How to build, test and run

```bash
./gradlew build            # compile
./gradlew test             # unit tests (network-tagged tests skipped, so it works offline)
./gradlew test -Pnetwork   # also runs the live Open-Meteo check
./gradlew run              # start the app
```

Demo accounts are created on first run (password `pw`): `coach1`, `player1`, `parent1`, `admin1`.

## 6. Known limitations

- Data is **per machine**: the JSON file is local, so nothing is shared between devices yet. A hosted store is a later one-class swap.
- The calendar still shows hard-coded demo events until `view_schedule` lands (task 2.1).
- `manage_users` is not built yet, so roles are chosen at sign-up.
- One role per user (decision D5).

## 7. API notes

Open-Meteo (no key required) is our external API. It was called successfully from Java on 2026-10-29 and is documented in the README API section in the course required format (link, how it is used, whether it was called). Alternatives we considered are listed in `Plan.md` decision D7.