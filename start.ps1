param(
    [switch]$NoBuild,
    [int]$TimeoutSeconds = 180,
    [switch]$SharePublic
)

$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

$appUrl = "http://localhost:8080"

$composeArgs = @("compose", "up", "-d")
if (-not $NoBuild) {
    $composeArgs += "--build"
}

Write-Host "Starting ScaleMart services..."
docker @composeArgs
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start docker compose services."
}

Write-Host "Waiting for app readiness at $appUrl ..."
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$ready = $false

while ((Get-Date) -lt $deadline) {
    try {
        $statusCode = (& curl.exe -s -o NUL -w "%{http_code}" $appUrl).Trim()
        if ($statusCode -eq "200") {
            $ready = $true
            break
        }
    } catch {
        # retry until timeout
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    throw "App did not become ready within $TimeoutSeconds seconds."
}

Write-Host "App is ready. Opening browser..."
Start-Process $appUrl

try {
    $lanIp = Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            $_.PrefixOrigin -ne "WellKnown"
        } |
        Select-Object -ExpandProperty IPAddress -First 1

    if ($lanIp) {
        Write-Host "LAN URL (same Wi-Fi/LAN): http://$lanIp`:8080"
    }
} catch {
    # ignore LAN IP detection failures
}

if ($SharePublic) {
    if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
        Write-Warning "npx is not installed. Install Node.js to use public sharing."
    } else {
        Write-Host "Starting public tunnel in a new PowerShell window..."
        Start-Process powershell -ArgumentList @(
            "-NoExit",
            "-Command",
            "npx localtunnel --port 8080"
        )
        Write-Host "Share the URL shown in the tunnel window."
    }
}

Write-Host "Done. You can monitor logs with: docker compose logs -f"
