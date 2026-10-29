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
API Ideas (free, no OAuth, no/minimal setup)
    - TheSportsDB — free sports data API (test key 3 built into the URL). Pull real team info, league schedules, player photos, past scores. Use case: let coaches link their team to a real pro team to display upcoming pro games, or import player positions/roles.
    - Open-Meteo — weather API, no key at all. Use case: show the weather forecast for each scheduled practice/game based on date + location, and flag events at risk of rain cancellation.
    - Nominatim (OpenStreetMap) — free geocoding, no key. Use case: convert typed venue addresses into coordinates, validate locations, or compute which field/venue is closest.
    - grp.ca / Nager.Date — public holidays API, no key. Use case: warn coaches when they schedule a practice on a stat holiday or long weekend.
    - API-Ninjas (Exercises endpoint) — free tier with just an API key in a header (signup takes a minute, no OAuth). Use case: coaches generate drill/workout suggestions by muscle group or sport for practice plans.
    - Firebase Realtime Database REST API / the course’s user-login DB API — plain HTTP GET/PUT with JSON, no OAuth. Use case: satisfies both the external API requirement and the data persistence requirement (store rosters, schedules, RSVPs as JSON shared across devices).


## Screenshots or animations demonstrating current functionality