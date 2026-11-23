# Team Sports — Feature Proposal

## Overview
Team Sports is a desktop application that keeps a sports team's schedule, attendance, stats and team information in one place. Coaches plan and publish practices and games, players see their commitments and RSVP, parents follow their child's schedule, and admins keep rosters and accounts organised. All four roles share one interface, and each role sees only the pages it needs.

## How we're building it (the pattern behind every feature)
Each feature is one **Clean Architecture vertical slice**: `use_case/<name>/` → `interface_adapter/<name>/` → a view, wired in `AppBuilder`. The slice already exists as a template (`create_event`), and work is split across **5 lanes and 5 sprints**, with a non-author review before anything reaches `main` *(Plan.md → "How this plan is organised", "Lanes and ownership")*.

---

## 1. Accounts — sign up and log in
**What it is.** A user creates an account and logs in; the app remembers who they are for the rest of the session. **Status: built** (Sprint 1).
**How we'll implement it.** Four use cases, each its own Clean Architecture slice with input/output boundaries, an interactor, a controller, a presenter and (where the page holds state) a view model: `login`, `signup`, `logout` and `change_password`. The signed-in user sits behind a `CurrentUserInterface` port implemented by `Session`, so the interactors own every session transition. Accounts are stored through the shared store - a hosted Supabase Postgres database when a project is configured, the local JSON file otherwise - and passwords are kept as PBKDF2 hashes. Sign-up takes a username, display name, password and role from a dropdown. *(Plan.md → 1.2, 1.3, 2.10, 3.10, 4.8, UC-A1 to UC-A4, lane L3 - done; storage decision D6, revised 2026-11-23.)*

## 2. Roles and role-based navigation
**What it is.** Four roles — coach, player, parent, admin — and each one sees only the pages that make sense for them. **Status: built** (Sprint 1).
**How we'll implement it.** The logged-in role lives in `Session`; `AppShell` rebuilds the sidebar whenever the session changes, showing only allowed pages. The role→page map is written down in `Lane-L3.md`. *(Plan.md → 1.5, lane L3, Sprint 1 — done; one-role-per-user is decision D5.)*

## 3. Creating events — practices and games
**What it is.** A coach schedules a practice or a game with a date, time, location and (for games) an opponent.
**How we'll implement it.** The existing `create_event` slice (controller → interactor → data access → presenter → view) already validates the team, the future date and the location. Events are saved to the shared store, so they survive a restart (the calendar still shows demo data until task 2.1). *(Plan.md → 1.6 and lane L1, Sprints 1–2.)*

## 4. Calendar / schedule view
**What it is.** A month and week view of everything the team has coming up, so nobody has to check a group chat.
**How we'll implement it.** A new `view_schedule` use case replaces the calendar's hard-coded demo events with real stored events, following the same slice pattern. *(Plan.md → 2.1, lane L1, Sprint 2.)*

## 5. Events list, details, edit and cancel
**What it is.** A list of all events with filters, a details page for a single event, and the ability to change or cancel it.
**How we'll implement it.** A list page replaces the placeholder, plus `edit_event` and `cancel_event` use cases; cancelled events are flagged wherever they appear. *(Plan.md → 2.2, 2.3, lane L1, Sprint 2.)*

## 6. RSVP
**What it is.** A player marks whether they are attending an upcoming practice or game, so the coach knows who is coming.
**How we'll implement it.** An `rsvp` use case stores each response and shows it to the coach. *(Plan.md → 2.5, lane L2, Sprint 2; supports user story 3.)*

## 7. Attendance
**What it is.** After a practice or game, the coach records who was present, absent or excused.
**How we'll implement it.** A `record_attendance` use case writes one `AttendanceRecord` per player per event into the shared store, reusing the existing `AttendanceRecord` entity. *(Plan.md → 2.4, lane L2, Sprint 2; user story 2.)*

## 8. Player stats and history
**What it is.** A player sees their own attendance history and performance over the season.
**How we'll implement it.** A `view_stats` use case reads a player's attendance records and shows them on a "My Stats" page. Whether stats stay free text (`statsNotes`) or become structured per-sport numbers is decision **D1**, taken in the Sprint 1 decision gate. *(Plan.md → 3.1, 3.2, lane L2, Sprint 3; user story 4.)*

## 9. Teams and roster management
**What it is.** An admin creates teams, assigns a coach, and adds or removes players.
**How we'll implement it.** A `manage_teams` use case behind a working Roster page, using the existing `Team` entity (one coach, many players). *(Plan.md → 2.6, lane L4, Sprint 2; user story 7.)*

## 10. Parent access — child's schedule and absence notices
**What it is.** A parent follows their child's schedule and tells the coach in advance when the child can't attend.
**How we'll implement it.** First a parent↔child link, then a `submit_absence_notice` use case that the coach can see. How the link is made, and whether a parent can have several children, are decisions **D3** and **D4**. *(Plan.md → 3.3, 3.4, lanes L4 + L5, Sprint 3; user story 6.)*

## 11. Announcements and notifications
**What it is.** Coaches post updates, and players and parents are told when an event is added, changed or cancelled.
**How we'll implement it.** An `Announcement` entity with a `post_announcement` use case, plus a notification feed that reacts to event changes. Whether announcements are standalone posts or only event-change notices is decision **D2**. *(Plan.md → 2.7, 2.8, lane L5, Sprint 2; user story 5.)*

## 12. Export the schedule
**What it is.** The team's schedule can be exported as a `.ics` file and imported into Google Calendar, Outlook or Apple Calendar.
**How we'll implement it.** The working `IcsExporter` file writer is promoted into a proper `export_schedule` use case behind a gateway, so it runs from the calendar UI instead of the view layer. *(Plan.md → 3.6, lane L1, Sprint 3.)*

## 13. Weather on events (external API)
**What it is.** Each scheduled event shows the forecast for its date and location, and events at risk of rain are flagged.
**How we'll implement it.** `OpenMeteoWeatherGateway` (already written and proven to work from Java) is surfaced through a controller and presenter on the event details page, and a second use case, `flag_rain_risk`, flags at-risk events on the calendar and the events list. *(Plan.md → 3.7 and 3.12, UC-F1 and UC-F2, lane L5, Sprint 3.)*

## 14. Admin — managing user accounts
**What it is.** An admin creates or deactivates accounts and changes a user's role.
**How we'll implement it.** A `manage_users` use case on an admin-only page, reading and writing the same store that login uses - the hosted database, so an admin sees every account rather than one device's file. *(Plan.md → 3.5, lane L3, Sprint 3; user story 8.)*

---

## Cross-cutting pieces

**Persistence.** A hosted **Supabase Postgres** database holds users, teams, events, attendance and announcements, reached over its REST API through the per-use-case data-access interfaces, so no use case knows which store it is talking to and one save touches one row. The local JSON file remains the offline default when no project is configured, and the connection settings are saved per user from inside the app, so no key enters the repository and nobody needs a terminal. *(Plan.md → 1.2, 4.8, decision D6 revised 2026-11-23, lane L3.)*

**External API.** Open-Meteo (no key required) is our documented external API: it was called successfully from Java on 2026-10-29 and is written up in the README's API section in the course's required format (link, how it's used, whether it was called). *(Plan.md → 1.9, decision D7.)*

**Testing.** JUnit 5 is wired into the build; each slice ships with its own tests, and a second person verifies the suite at the end of Sprints 2 and 3. *(Plan.md → 1.8, 2.9, 3.9.)*

**Documentation.** `Documentation.md` (layer diagram, sequence diagrams, class diagram, CRC cards, API notes) and the README are split across the team, one named deliverable per lane, so nobody carries documentation alone. *(Plan.md → 1.7, 3.8, 4.5, 4.6.)*

**Delivery.** Sprints 4 and 5 are integration and hardening: a cross-lane bug bash, a UI consistency pass, demo seed data, a rehearsal, and a clean-clone verification of `main`. *(Plan.md → 4.1–4.7, 5.1–5.3.)*

## Not in scope for this term
`manage_users` and weather/`.ics` export are the lowest priority (P2) and are the first things to be cut if time runs short. Structured per-sport stats, multi-child parent links and standalone announcements depend on decisions D1–D4. *(Plan.md → "Priority if time runs short", "Open decisions".)*
