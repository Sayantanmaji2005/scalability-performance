param(
    [string]$EnvFile = ".env.production",
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

if (-not (Test-Path $EnvFile)) {
    throw "Missing env file '$EnvFile'. Create it from .env.production.example first."
}

# Enforce secure/default production bind addresses unless explicitly overridden.
if (-not $env:NGINX_HTTP_BIND) { $env:NGINX_HTTP_BIND = "80:80" }
if (-not $env:NGINX_HTTPS_BIND) { $env:NGINX_HTTPS_BIND = "443:443" }
if (-not $env:PROMETHEUS_BIND) { $env:PROMETHEUS_BIND = "127.0.0.1:9090:9090" }
if (-not $env:GRAFANA_BIND) { $env:GRAFANA_BIND = "127.0.0.1:3000:3000" }

$args = @("--env-file", $EnvFile, "-f", "docker-compose.yml", "-f", "docker-compose.prod.yml", "up", "-d")
if (-not $NoBuild) {
    $args += "--build"
}

Write-Host "Starting production profile with TLS reverse proxy..."
docker compose @args
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start production profile."
}

Write-Host "Production profile started."
Write-Host "Public URL: https://<your-domain>"
Write-Host "Prometheus local-only: http://127.0.0.1:9090"
Write-Host "Alertmanager local-only: http://127.0.0.1:9093"
Write-Host "Grafana local-only: http://127.0.0.1:3000"
