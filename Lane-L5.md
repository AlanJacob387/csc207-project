# Daniel — communication and weather

Reviewed on November 9, 2026 against `main` at `7a754ee`.
Working branch: `daniel/communications-weather`.

Daniel owns **L5**, as assigned in `Plan.md:22` and the story ownership table at
`Plan.md:266–267`. `Lane-L3.md` belongs to Andy. This review records proposed
work; it does not change team decisions, ownership, or completion status.

## Announcement implementation on this branch

Following Daniel's request to implement standalone announcements, posting and
viewing are implemented on this branch. The earlier review below remains a
snapshot of the starting point at `7a754ee`.

- A coach can post only to a team they currently coach. Coaches read their own
  teams, players read rostered teams, and admins can read all teams. Identity
  comes from the current session; the use cases recheck the stored role and
  membership for each request.
- Announcements appear newest first with author and posting time. The page
  supports team selection, refresh, empty states, and posting feedback. Failed
  posts retain the draft; signing out or changing users clears previous content.
- Posts persist in the `announcements` table when the hosted database is
  configured, and otherwise in an added `announcements` array in
  `data/app-data.json`. Older stores without that array still load. Saves
  preserve existing records and unrecognised sections/fields, reject duplicate
  announcement IDs, and stage complete file contents before replacement. Failed
  announcement writes do not publish the post in memory.
- Parent reads remain unavailable until the parent-to-player link contract
  exists. Event-change notifications and absence notices remain separate work.
- `AppBuilder` and `Main` contain narrow page-wiring changes. Shared-file and
  contract changes still need the owning lanes' review before merging; no
  teammate sign-off is claimed here.

Validation: `bash ./gradlew --offline build` passes **96 tests** with no failures
or errors. The added coverage includes form posting, failed-save draft retention,
user changes, multiple-team selection, and long-message wrapping at 1000, 650,
and 440 pixels. A separate headless rendering check confirmed that long posts
remain reachable by scrolling. All persistence fixtures used temporary stores;
the default application data was not accessed. The earlier startup seed and
legacy account-save memory findings remain separate follow-up work.

## What exists

- Login, signup, role-filtered navigation, shared database persistence (local
  file fallback), and the create-event slice are implemented. Login/signup are
  the useful slice examples.
- `AppBuilder.java:112–113` still registers an Announcements placeholder.
  There are no announcement, notification, absence-notice, or weather use-case
  packages, and no entities/storage for the three communication features.
- `OpenMeteoWeatherGateway` already builds requests, parses a daily forecast,
  and flags rain probabilities of at least 60%. Its five offline tests pass.
  It is not connected to an event page or calendar.
- `CalendarView.java:134` populates demo events; `AppBuilder.java:110` supplies
  no schedule controller. Event details are still a placeholder at
  `CalendarView.java:401`. Created events persist but do not populate this view.
- `Event.java:15` stores a venue as text. Weather requires latitude/longitude,
  so a location-to-coordinates contract is still needed.
- `Documentation.md` contains only a heading. README lacks build/run/test and
  demo-account instructions, despite these being Daniel's assigned contribution.

## Work order

Dates below are the detailed task-table targets in `Plan.md`, not new deadlines.

| Order | Work | Existing target | Concrete result and dependencies |
|---|---|---|---|
| 1 | Resolve D2 and freeze communication contracts | Decision gate was October 31 | Daniel proposes standalone announcements versus event-change notices only; record the team's decision. Agree entity fields and persistence with Aarav/Andy, recipient rules with Aarav, and event-change hooks with Alan. Shared contracts need two-lane sign-off. |
| 2 | `post_announcement` and `view_announcements` (UC-E1/E2) | November 10, task 2.7 | If standalone posts are confirmed, deliver coach-authorized posting and a team-filtered list. Start with narrow interfaces, interactors, and tests against a fake; then mapper/file-store support and the page. Alan adds the builder wiring. |
| 3 | `view_notifications` (UC-E3) | November 11, task 2.8 | Show created/changed/cancelled event notices to the correct players and parents. Alan's event mutations must supply changes; Aarav's team and parent-link contracts supply recipients. Persist notices, prevent duplicate delivery, and test team isolation. |
| 4 | README build/test instructions | L5 documentation assignment | Document prerequisites, wrapper commands, default versus live tests, local-only data, demo accounts, and actual functionality. Correct the claim that weather is already visible. This can proceed while contracts are being agreed. |
| 5 | `submit_absence_notice` and `view_absence_notices` (UC-E4/E5) | November 18, task 3.4 | Build parent-authorized submission and coach-scoped viewing. Depends on Aarav's parent-to-player link (3.3). Test using a narrow fake while that slice is built. An advance absence notice is separate from the coach's attendance record. |
| 6 | `view_event_weather` (UC-F1) | November 18, task 3.7 | Add the use-case gateway interface, interactor, adapters, and forecast state. Agree coordinates with Aarav/Alan and event-details integration with Alan (2.3). Show loading, unavailable, and usable forecasts without blocking Swing. |
| 7 | `flag_rain_risk` (UC-F2) | November 18, task 3.12; stretch | Add calendar/list flags once forecasts and real event views exist. Keep 60% as the documented threshold unless the team changes it. Unknown data must stay unknown rather than appear safe. |

The Sprint 3 non-author verification task (3.9) is also Daniel's, due November
18. Verify teammates' slices; another teammate must verify Daniel's work.
Daniel also coordinates sequence diagrams (3.8, November 18) and contributes
demo seed data, the demo script, and the README/documentation final pass
(4.4–4.6, November 24).

## First implementation slice

After D2 is agreed, start with announcement posting and reading. A proposed
minimum record is an ID, team ID, author username, message, and creation time;
these fields need team agreement before becoming a shared entity.

For `post_announcement`, test an authorized coach's successful post, blank
content, unknown team, and a user who does not coach that team. Handle storage
failure without reporting success. For `view_announcements`, test a team's
ordered posts, an empty list, and denied access to another team's posts.

Deliver each use case through the team's six use-case files, three adapter
files, and a wired view. Commit the business logic and tests separately from
the UI. Add persistence round-trip checks proving old users, teams, events, and
attendance survive the schema addition, and that posts survive reopening the
store. Get a non-author review before merging.

For notifications, agree whether the persisted change is captured at event
creation/edit/cancellation or through a durable change log. Do not reconstruct
changes solely from the latest event state. Parent notifications have a real
dependency on parent links even though task 2.8's dependency cell omits it.

## Code findings to resolve or hand off

These are code-review findings. The build result below does not prove these
paths are covered by the current tests.

| Priority | Finding and evidence | Action / owner |
|---|---|---|
| High | Startup can overwrite records. `AppBuilder.java:208–219` checks only for `coach1`, then upserts `player1`, `parent1`, `admin1`, and `demo-team`. A store missing only `coach1` can lose existing passwords, profiles, or its team roster. | Alan/Andy: make demo initialization insert-only or explicitly separate it from real stores. Verify customized records survive startup. **Resolved 2026-11-23:** each demo account is inserted only when that username is missing, the demo team only when its id is absent, and demo passwords are hashed as they are written. |
| High | Failed writes leave memory changed. `FileAppDataAccessObject.java:86–88` puts a user into memory before persistence; the same pattern exists for other writes. A reported failed signup can still make that account usable until restart. Writes at `:288` also replace the only data file directly. | Andy: commit memory only after a successful durable write, or roll back; use a temporary sibling file and atomic replacement where supported. Add failed-write preservation tests before extending persistence for L5. |
| High | A present, wrong-typed top-level JSON field is silently treated as empty by `FileAppDataAccessObject.java:295–298`. The next save rewrites that section and can discard data. | Andy: reject invalid schema while preserving the original file; distinguish absent collections from corrupt ones. Add a startup error/recovery path because eager loading at `AppBuilder.java:55` currently aborts startup on malformed storage. |
| Medium | Missing forecast numbers become `NaN` temperature and `-1` precipitation (`OpenMeteoWeatherGateway.java:139–142`); `isRainRisk()` returns false for that unavailable precipitation (`:83–84`). HTTP success is not checked before parsing (`:101–106`). | Daniel: introduce explicit unavailable data and transport errors. Add offline tests for null/short arrays, invalid values, absent dates, HTTP failures, and the 59/60% boundary. |
| Medium | A failed create-event submission followed by a successful one still displays the previous error. `CreateEventPresenter.java:18–24` sets only one message and `CreateEventView.java:78–81` prefers the retained error. | Alan: make success/error state mutually exclusive and add a failed-then-success regression case. |
| Medium | The calendar and its export read a demo map rather than stored events (`CalendarView.java:134,229`). There is no create-event test package, despite the slice being marked done. | Alan: implement `view_schedule` and refresh after changes. Aidan/Alan: finish task 1.8 with interactor success/failure coverage. These are prerequisites for dependable event notifications and weather integration. |
| Low | Export assigns an overnight event's end to the original date (`IcsExporter.java:73–75`), so a 23:30 event can end at 00:30 before it starts. | Alan: calculate the end as a date-time and cover midnight rollover when building `export_schedule`. |

The signup role dropdown remains a documented demo choice in `Lane-L3.md`.
Passwords are no longer plaintext: they are stored as PBKDF2 hashes since
2026-11-23. Changes to either policy belong with Andy and the team.

## Planning inconsistencies to reconcile

- `Plan.md:5,48` still calls L5 unnamed; the lane table and latest ownership
  commit assign Daniel.
- `Proposal.md` assigns weather to L1 and documentation end to end to L5.
  The current plan assigns weather to Daniel and splits documentation by lane.
- The sprint overview places parents/weather in Sprint 4, while the detailed
  tasks put Daniel's absence/weather work in Sprint 3. The catalogue and final
  cut list also disagree on priorities. Confirm the intended dates and cut order.
- The old backlog says signup is missing, and the risks table says no build
  exists. Both are already implemented.
- `README.md:35` claims forecasts appear in schedules; only the gateway exists.
  The plan marks README build instructions complete, but they are absent.

These discrepancies are recorded here without rewriting the shared plan or
another contributor's documents.

## Verification baseline

On November 9, 2026, using Temurin JDK 21.0.8:

```sh
bash ./gradlew --offline build
```

Result: **build passed; 42 tests, 0 failures, 0 errors**. Dependencies were
already cached. Network-tagged tests were excluded by the build configuration;
no live weather request was made. This verifies the existing suite, not the
unimplemented L5 features or a full interactive application flow.

Two additional checks used a temporary Java harness and temporary working
directories outside the repository. Startup changed an existing `player1`
password/profile and replaced a customized `demo-team` when `coach1` was absent.
A signup targeting an unwritable store reported failure but left the account in
memory, and a subsequent login succeeded despite no data file being created.
Both checks reproduced the findings above. The harness and temporary data were
removed; the repository's default store was not created or modified.

The wrapper is now tracked with its executable bit (`100755`), so `./gradlew`
works on a clean macOS/Linux checkout.
Demo usernames are `coach1`, `player1`, `parent1`, and `admin1`, all seeded with
password `pw`; the startup seed is now insert-only per account, so an existing
account is never overwritten. Runtime state lives in the hosted database when it
is configured, and in the ignored `data/app-data.json` file otherwise.

This review adds project instructions, a workflow skill, and this backlog only.
Existing source files, shared documents, and runtime data are preserved.
