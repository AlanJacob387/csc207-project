# Team Project

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.

## Summary

Team Sports is a desktop application that brings everything a sports team needs into one shared space: schedules, announcements, attendance, performance stats, and roster information. Coaches can plan and publish practices and games with dates, times, and locations, record attendance and player stats after each one, and send updates to the whole team. Players can view their upcoming commitments, RSVP so the coach knows who is attending, and track their own attendance and progress over the season. Parents can follow their child's schedule and submit absence notices ahead of time, so nothing is missed. By supporting multiple sports and multiple roles within a single unified interface, the platform replaces scattered group chats, spreadsheets, and paper sign-ups with one organized place for the whole team.

*Refer to (`Proposal.md`) for a more detailed description*

## User stories

| # | User story | Owner |
|---|-----------|-------|
| 1 | As a coach, I want to create and schedule practices and games (date, time, location) so that my team always knows when and where to show up all in one platform. | Alan (L1) |
| 2 | As a coach, I want to record player attendance and performance stats after each practice or game so that I can track player development over the season. | Aidan (L2) |
| 3 | As a player, I want to view my team's upcoming schedule and RSVP to practices and games so that my coach knows whether I'll be attending. | Alan + Aidan (L1/L2) |
| 4 | As a player, I want to view my personal stats and attendance history so that I can monitor my own progress. | Aidan (L2) |
| 5 | As a parent, I want to receive notifications when events are added, changed, or cancelled so that I can stay informed about my child's commitments. | L5 |
| 6 | As a parent, I want to view my child's schedule and submit absence notices so that the coach is informed ahead of time. | Aarav + L5 (L4/L5) |
| 7 | As an admin, I want to create teams and assign coaches and players to them so that the platform's rosters stay organized and up to date. | Aarav (L4) |
| 8 | As an admin, I want to manage user accounts and roles (coach, player, parent) so that each user only has access to the features appropriate to them. | Andy (L3) |

The individual use cases behind each story are catalogued in `Plan.md`.

## APIs

### Open-Meteo — weather forecast (in use)
- **Documentation:** https://open-meteo.com/en/docs
- **How it is used:** the schedule shows the forecast for the date and place of each practice or game, and an event whose chance of rain is 60% or higher is flagged as at risk of cancellation. Code: `data_access/OpenMeteoWeatherGateway`.
- **Called successfully?** **Yes.** First successful call from Java (OkHttp) on 2026-10-29: `Open-Meteo replied for 2026-10-29: max 21.3 C, rain chance 19%`. No key or account needed. Live check: `gradlew test -Pnetwork` (network-tagged tests are skipped by default so the suite stays green offline).

### Supabase (Postgres + REST) — the shared database (in use)
- **Documentation:** https://supabase.com/docs/guides/api
- **How it is used:** accounts, teams, events, attendance and announcements live in a Supabase Postgres database and are read and written through its REST API (PostgREST): a read filters on the key, and a save posts to `?on_conflict=<key>` with `Prefer: resolution=merge-duplicates`. Code: `data_access/SupabaseAppDataAccessObject`; schema in `db/schema.sql`; setup in `db/README.md`. The connection comes from the in-app **Database settings** page (stored in `%USERPROFILE%\.team-sports\database.properties`) or from `TEAMSPORTS_DB_URL` / `TEAMSPORTS_DB_KEY`. Only the **publishable** key is used - a secret key bypasses row level security and the app refuses one.
- **Called successfully?** **Yes.** First verified against the project on 2026-11-23: all five tables answered `200 []`, and `/rest/v1/` answered `401 Secret API key required`, which is the healthy answer for that route and proves PostgREST is reachable. Live check: `gradlew test -Pnetwork`.

### Ideas we have not called yet
- **TheSportsDB** — https://www.thesportsdb.com/free_sports_api — link a team to a real pro team and import names or photos (free test key).
- **Nominatim (OpenStreetMap)** — https://nominatim.org/release-docs/latest/api/Overview/ — turn a typed venue address into coordinates.
- **Nager.Date** — https://date.nager.at/swagger/index.html — warn when a practice lands on a holiday or long weekend.
- **API-Ninjas (Exercises)** — https://api-ninjas.com/api/exercises — drill suggestions for practice plans (free key in a header).

### Where data lives
- **Shared:** accounts, teams, events, attendance and announcements live in the Supabase database above, so every device that connects with the same project sees the same data.
- **Local fallback:** with no database configured the app uses `data/app-data.json` (git-ignored), which is also what the unit tests use. The sign-in page always shows which store is active.
- **Connection settings are per user and per machine**, outside the repository: `%USERPROFILE%\.team-sports\database.properties` on Windows, `~/.team-sports/database.properties` on macOS and Linux.
- **If a store is not usable the app says so.** A database that cannot be reached leaves the app on the sign-in page with the reason instead of switching to the local file, and a `data/app-data.json` that cannot be parsed is kept beside the new file as `app-data.json.corrupt-<timestamp>` while the app starts empty.

## How to run

Needs JDK 17 or newer, with `JAVA_HOME` pointing at it.

```bash
./gradlew build             # compile
./gradlew test              # unit tests (network tests are skipped, so this works offline)
./gradlew test -Pnetwork    # also runs the live Open-Meteo and database checks
./gradlew run               # start the app
```

Demo accounts are created automatically the first time the app runs (password `teampass`): `coach1` (coach), `player1` (player), `parent1` (parent), `admin1` (admin). Their passwords are stored hashed. Each role sees only the pages it may use, an administrator creates, re-roles, deactivates and reactivates accounts on **Manage Users**, and **Create an Account** makes a new one - a username of 3 to 40 characters and a password of 8 to 128, the same rule on every page that writes a credential.

To use the shared database, open **Database settings** on the sign-in page and paste the project URL and publishable key (see `db/README.md`) - nothing is needed in a terminal. Without it the app keeps its data in `data/app-data.json`, which is git-ignored.

## Screenshots or animations demonstrating current functionality
<img width="2880" height="1772" alt="image" src="https://github.com/user-attachments/assets/5bedcc77-7dd2-4c83-a515-e4efa756f474" />

<img width="2868" height="1788" alt="image" src="https://github.com/user-attachments/assets/07858b7f-88b0-45ed-9d8b-ad59fec3ea79" />

<img width="2880" height="1800" alt="image" src="https://github.com/user-attachments/assets/a7cd7aba-fab7-4b51-9264-8a6e0b044442" />
