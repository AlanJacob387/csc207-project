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
    1. As a coach, I want to create and schedule practices and games (date, time, location) so that my team always knows when and where to show up all in one platform.
    2. As a coach, I want to record player attendance and performance stats after each practice or game so that I can track player development over the season.
    3. As a player, I want to view my team’s upcoming schedule and RSVP to practices and games so that my coach knows whether I’ll be attending.
    4. As a player, I want to view my personal stats and attendance history so that I can monitor my own progress.
    5. As a parent, I want to receive notifications when events are added, changed, or cancelled so that I can stay informed about my child’s commitments.
    6. As a parent, I want to view my child’s schedule and submit absence notices so that the coach is informed ahead of time.
    7. As an admin, I want to create teams and assign coaches and players to them so that the platform’s rosters stay organized and up to date.
    8. As an admin, I want to manage user accounts and roles (coach, player, parent) so that each user only has access to the features appropriate to them.

## APIs

### Open-Meteo — weather forecast (in use)
- **Documentation:** https://open-meteo.com/en/docs
- **How it is used:** the schedule shows the forecast for the date and place of each practice or game, and an event whose chance of rain is 60% or higher is flagged as at risk of cancellation. Code: `data_access/OpenMeteoWeatherGateway`.
- **Called successfully?** **Yes.** First successful call from Java (OkHttp) on 2026-10-29: `Open-Meteo replied for 2026-10-29: max 21.3 C, rain chance 19%`. No key or account needed. Live check: `gradlew test -Pnetwork` (network-tagged tests are skipped by default so the suite stays green offline).

### Ideas we have not called yet
- **TheSportsDB** — https://www.thesportsdb.com/free_sports_api — link a team to a real pro team and import names or photos (free test key).
- **Nominatim (OpenStreetMap)** — https://nominatim.org/release-docs/latest/api/Overview/ — turn a typed venue address into coordinates.
- **Nager.Date** — https://date.nager.at/swagger/index.html — warn when a practice lands on a holiday or long weekend.
- **API-Ninjas (Exercises)** — https://api-ninjas.com/api/exercises — drill suggestions for practice plans (free key in a header).
- **Firebase Realtime Database REST** — https://firebase.google.com/docs/reference/rest/database — a shared JSON store instead of the local file.

### Where data lives
- App data (accounts, teams, events, attendance) is kept in a local JSON file (`data/app-data.json`), so it is **per machine** - nothing is shared between devices yet. A hosted store can be added later behind the same interfaces.

## Screenshots or animations demonstrating current functionality
<img width="2880" height="1772" alt="image" src="https://github.com/user-attachments/assets/5bedcc77-7dd2-4c83-a515-e4efa756f474" />

<img width="2868" height="1788" alt="image" src="https://github.com/user-attachments/assets/07858b7f-88b0-45ed-9d8b-ad59fec3ea79" />

<img width="2880" height="1800" alt="image" src="https://github.com/user-attachments/assets/a7cd7aba-fab7-4b51-9264-8a6e0b044442" />
