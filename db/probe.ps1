# Prints what the configured Supabase project answers, so a 404 can be explained.
#
#   auth health 200 + rest root 401 + table 404  -> the project is fine, the schema was never applied
#   auth health 200 + rest root 404              -> the Data API is disabled for the project
#   auth health fails                            -> wrong URL, or the free project is paused
#
# Usage, after setting the two variables (see db/README.md):
#   ./db/probe.ps1

$base = $env:TEAMSPORTS_DB_URL
$key = $env:TEAMSPORTS_DB_KEY

if (-not $base -or -not $key) {
    Write-Host 'Set TEAMSPORTS_DB_URL and TEAMSPORTS_DB_KEY first (see db/README.md).'
    exit 1
}

$base = $base.TrimEnd('/')
Write-Host "Project: $base"

function Show-Response {
    param([string] $Name, [string] $Url, [hashtable] $Headers)

    try {
        $response = Invoke-WebRequest -Uri $Url -Headers $Headers -UseBasicParsing
        $body = "$($response.Content)"
        Write-Host ("{0,-14} HTTP {1} :: {2}" -f $Name, $response.StatusCode, $body.Substring(0, [Math]::Min(240, $body.Length)))
    }
    catch {
        $code = $_.Exception.Response.StatusCode.value__
        $body = $_.ErrorDetails.Message
        if (-not $body) { $body = $_.Exception.Message }
        Write-Host ("{0,-14} HTTP {1} :: {2}" -f $Name, $code, $body.Substring(0, [Math]::Min(240, $body.Length)))
    }
}

$apiKey = @{ apikey = $key }

# No key needed: proves the project ref is right and the project is not paused.
Show-Response 'auth health' "$base/auth/v1/health" @{}
# The OpenAPI root only accepts a secret key, so 401 here means "PostgREST is alive".
Show-Response 'rest root' "$base/rest/v1/" $apiKey

foreach ($table in @('users', 'teams', 'events', 'attendance', 'announcements')) {
    Show-Response $table "$base/rest/v1/$table`?select=*&limit=1" $apiKey
}
