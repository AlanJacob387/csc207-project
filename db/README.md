# Hosted database (Supabase)

The app keeps accounts, teams, events, attendance and announcements in a
Supabase Postgres database, reached over its REST API (PostgREST). The local
JSON file is still used when no database is configured, so a fresh clone runs
without any setup.

## 1. Create the project

1. Sign in at <https://supabase.com/dashboard> (GitHub or Google; the free plan
   needs no card).
2. **New project**: pick your organization, name it `team-sports`, choose a
   region close to you, and set a database password. Save that password in your
   password manager - it is for direct SQL access only, the app never uses it.
3. Wait until provisioning finishes (a minute or two).

## 2. Copy the two values the app needs

| Value | Where | Looks like |
|---|---|---|
| Data API URL | Dashboard -> **Integrations -> Data API** | `https://<project-ref>.supabase.co` |
| Publishable key | Dashboard -> **Settings -> API Keys** | `sb_publishable_...` |

Use the **publishable** key, never the secret key. Supabase is retiring the old
`anon` / `service_role` keys, and a secret key bypasses Row Level Security, so it
must never be shipped in a desktop application.

## 3. Create the tables

Open the **SQL Editor**, paste the whole of [`schema.sql`](schema.sql), and run
it. It is safe to re-run.

Check that it worked:

```sql
select tablename, rowsecurity from pg_tables where schemaname = 'public';
select tablename, policyname from pg_policies where schemaname = 'public';
```

Every table must show `rowsecurity = true` and three policies.

## 4. Point the app at it

PowerShell, current session only:

```powershell
$env:TEAMSPORTS_DB_URL = 'https://<project-ref>.supabase.co'
$env:TEAMSPORTS_DB_KEY = 'sb_publishable_...'
./gradlew run
```

To keep them, set them once for your user account:

```powershell
[Environment]::SetEnvironmentVariable('TEAMSPORTS_DB_URL', 'https://<project-ref>.supabase.co', 'User')
[Environment]::SetEnvironmentVariable('TEAMSPORTS_DB_KEY', 'sb_publishable_...', 'User')
```

With neither variable set, the app falls back to `data/app-data.json`, which is
what the unit tests and an offline demo use.

Verify the database itself from a terminal:

```powershell
Invoke-RestMethod -Uri "$env:TEAMSPORTS_DB_URL/rest/v1/users?select=username" -Headers @{ apikey = $env:TEAMSPORTS_DB_KEY }
```

An empty array means the table exists, the key works and the policies allow the
read. Demo accounts appear after the first app run.

## 5. Keep it awake

A free-plan project is **paused after 7 days without activity**, and a paused
project cannot answer the REST API. A few requests a day are enough; visiting
the dashboard also counts. If it does pause, the dashboard shows a **Resume
project** button, and the data is kept for a year. The app falls back to the
local file while the project is paused.

## Security notes

- Row Level Security is on for every table, and the policies grant only select,
  insert and update. There is no delete policy: accounts are deactivated, not
  deleted.
- Anyone who extracts the publishable key has the same access the app has. That
  is acceptable only because accounts store a PBKDF2 hash rather than a
  password, and because the data is a course project's demo data.
- Only edit `schema.sql`; the REST API is generated from the schema, so a column
  added here is reachable immediately.
