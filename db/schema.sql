-- Team Sports - schema for the hosted store (Supabase Postgres).
--
-- Column names deliberately match the JSON keys EntityJsonMapper already uses
-- ("displayName", "teamId", ...), so the local file store and the database keep
-- one shape and one mapper. Postgres folds unquoted identifiers to lower case,
-- so every mixed-case name below is quoted and is therefore case-sensitive,
-- including inside REST filters such as ?displayName=eq.Pat.
--
-- Every table uses the natural key the app already has. That is what makes
-- "POST /rest/v1/<table>?on_conflict=<key>" with
-- "Prefer: resolution=merge-duplicates" an upsert, which is exactly what
-- save(...) means in the data-access classes.
--
-- Apply once from the Supabase SQL Editor. Re-running is safe.

create table if not exists public.users (
    "username"    text primary key,
    -- Stored credential: "pbkdf2$<iterations>$<salt>$<hash>" for new accounts.
    -- Rows written before hashing existed hold a plaintext value and are still
    -- accepted by PasswordHasher so an existing store keeps working.
    "password"    text not null,
    "displayName" text not null,
    "role"        text not null
                  check ("role" in ('COACH', 'PLAYER', 'PARENT', 'ADMIN')),
    "active"      boolean not null default true
);

create table if not exists public.teams (
    "teamId"          text primary key,
    "name"            text not null,
    "sport"           text not null,
    "coachUsername"   text references public.users ("username") on delete set null,
    "playerUsernames" text[] not null default '{}'
);

create table if not exists public.events (
    "eventId"   text primary key,
    "teamId"    text not null references public.teams ("teamId") on delete cascade,
    "type"      text not null check ("type" in ('PRACTICE', 'GAME')),
    -- ISO-8601 local date-time such as 2026-11-01T18:00, which is what
    -- LocalDateTime.toString() writes. Kept as text so the mapper is unchanged.
    "dateTime"  text not null,
    "location"  text not null,
    "opponent"  text,
    "cancelled" boolean not null default false
);

create table if not exists public.attendance (
    "eventId"        text not null references public.events ("eventId") on delete cascade,
    "playerUsername" text not null references public.users ("username") on delete cascade,
    "status"         text not null
                     check ("status" in ('PRESENT', 'ABSENT', 'EXCUSED')),
    "statsNotes"     text,
    primary key ("eventId", "playerUsername")
);

create table if not exists public.announcements (
    "announcementId" text primary key,
    "teamId"         text not null references public.teams ("teamId") on delete cascade,
    "authorUsername" text not null references public.users ("username") on delete cascade,
    "message"        text not null,
    "createdAt"      text not null
);

-- An administrator links an existing parent account to an existing player
-- account; the link is what lets a parent see that child's team.
create table if not exists public.parent_links (
    "parentUsername" text not null references public.users ("username") on delete cascade,
    "playerUsername" text not null references public.users ("username") on delete cascade,
    primary key ("parentUsername", "playerUsername")
);

-- A parent's advance notice that a linked child will miss an event. One notice
-- per parent, child and event: a second submission updates the first.
create table if not exists public.absence_notices (
    "noticeId"       text primary key,
    "parentUsername" text not null references public.users ("username") on delete cascade,
    "playerUsername" text not null references public.users ("username") on delete cascade,
    "eventId"        text not null references public.events ("eventId") on delete cascade,
    "reason"         text not null,
    "submittedAt"    text not null,
    unique ("parentUsername", "playerUsername", "eventId")
);

-- What changed about an event (created, changed, cancelled), written when the
-- event is saved. Readers filter it to the teams they may see.
create table if not exists public.event_notifications (
    "notificationId" text primary key,
    "eventId"        text not null references public.events ("eventId") on delete cascade,
    "teamId"         text not null references public.teams ("teamId") on delete cascade,
    "changeType"     text not null,
    "message"        text not null,
    "createdAt"      text not null
);

-- Helpful indexes for the reads the app actually performs.
create index if not exists events_team_idx
    on public.events ("teamId", "dateTime");
create index if not exists announcements_team_idx
    on public.announcements ("teamId", "createdAt");
create index if not exists attendance_player_idx
    on public.attendance ("playerUsername");
create index if not exists absence_notices_event_idx
    on public.absence_notices ("eventId");
create index if not exists event_notifications_team_idx
    on public.event_notifications ("teamId", "createdAt");

-- ---------------------------------------------------------------------------
-- Access control
--
-- The desktop client ships the publishable key, so every request runs as the
-- "anon" role. Row Level Security is enabled on every table and the policies
-- below grant exactly what the app does. There is deliberately no delete
-- policy except on parent_links: the app deactivates accounts instead of
-- deleting rows, and only an administrator ending a family link removes one.
--
-- Course scope: anyone who extracts the key inherits the app's permissions.
-- That is why passwords are stored hashed instead of in plaintext.
-- ---------------------------------------------------------------------------

alter table public.users         enable row level security;
alter table public.teams         enable row level security;
alter table public.events        enable row level security;
alter table public.attendance    enable row level security;
alter table public.announcements enable row level security;
alter table public.parent_links  enable row level security;
alter table public.absence_notices     enable row level security;
alter table public.event_notifications enable row level security;

grant select, insert, update on public.users, public.teams, public.events,
      public.attendance, public.announcements, public.parent_links,
      public.absence_notices, public.event_notifications to anon, authenticated;
grant delete on public.parent_links to anon, authenticated;

drop policy if exists users_select on public.users;
create policy users_select on public.users
    for select to anon, authenticated using (true);
drop policy if exists users_insert on public.users;
create policy users_insert on public.users
    for insert to anon, authenticated with check (true);
drop policy if exists users_update on public.users;
create policy users_update on public.users
    for update to anon, authenticated using (true) with check (true);

drop policy if exists teams_select on public.teams;
create policy teams_select on public.teams
    for select to anon, authenticated using (true);
drop policy if exists teams_insert on public.teams;
create policy teams_insert on public.teams
    for insert to anon, authenticated with check (true);
drop policy if exists teams_update on public.teams;
create policy teams_update on public.teams
    for update to anon, authenticated using (true) with check (true);

drop policy if exists events_select on public.events;
create policy events_select on public.events
    for select to anon, authenticated using (true);
drop policy if exists events_insert on public.events;
create policy events_insert on public.events
    for insert to anon, authenticated with check (true);
drop policy if exists events_update on public.events;
create policy events_update on public.events
    for update to anon, authenticated using (true) with check (true);

drop policy if exists attendance_select on public.attendance;
create policy attendance_select on public.attendance
    for select to anon, authenticated using (true);
drop policy if exists attendance_insert on public.attendance;
create policy attendance_insert on public.attendance
    for insert to anon, authenticated with check (true);
drop policy if exists attendance_update on public.attendance;
create policy attendance_update on public.attendance
    for update to anon, authenticated using (true) with check (true);

drop policy if exists announcements_select on public.announcements;
create policy announcements_select on public.announcements
    for select to anon, authenticated using (true);
drop policy if exists announcements_insert on public.announcements;
create policy announcements_insert on public.announcements
    for insert to anon, authenticated with check (true);
drop policy if exists announcements_update on public.announcements;
create policy announcements_update on public.announcements
    for update to anon, authenticated using (true) with check (true);

drop policy if exists parent_links_select on public.parent_links;
create policy parent_links_select on public.parent_links
    for select to anon, authenticated using (true);
drop policy if exists parent_links_insert on public.parent_links;
create policy parent_links_insert on public.parent_links
    for insert to anon, authenticated with check (true);
drop policy if exists parent_links_update on public.parent_links;
create policy parent_links_update on public.parent_links
    for update to anon, authenticated using (true) with check (true);
drop policy if exists parent_links_delete on public.parent_links;
create policy parent_links_delete on public.parent_links
    for delete to anon, authenticated using (true);

drop policy if exists absence_notices_select on public.absence_notices;
create policy absence_notices_select on public.absence_notices
    for select to anon, authenticated using (true);
drop policy if exists absence_notices_insert on public.absence_notices;
create policy absence_notices_insert on public.absence_notices
    for insert to anon, authenticated with check (true);
drop policy if exists absence_notices_update on public.absence_notices;
create policy absence_notices_update on public.absence_notices
    for update to anon, authenticated using (true) with check (true);

drop policy if exists event_notifications_select on public.event_notifications;
create policy event_notifications_select on public.event_notifications
    for select to anon, authenticated using (true);
drop policy if exists event_notifications_insert on public.event_notifications;
create policy event_notifications_insert on public.event_notifications
    for insert to anon, authenticated with check (true);
drop policy if exists event_notifications_update on public.event_notifications;
create policy event_notifications_update on public.event_notifications
    for update to anon, authenticated using (true) with check (true);

-- Check the result from the SQL Editor; both should return empty result sets.
--   select tablename, rowsecurity from pg_tables where schemaname = 'public';
--   select tablename, policyname from pg_policies where schemaname = 'public';
--
-- Demo accounts are NOT seeded here on purpose: the app inserts them through
-- UserFactory on first run (insert-only), so their passwords are hashed with
-- the same code path as a real signup.
