# Builds every loader folder of this branch (fabric/, and neoforge/ and forge/ where they exist) with its own Gradle
# wrapper; stops at the first failure and prints the jars it built.
# Loaders are discovered generically: any top-level folder that contains settings.gradle and gradlew.
# Usage: ./build-all.ps1 [gradle tasks and options...]   (default: build)
# Every gradlew call runs with --no-daemon (added unless the caller already passed it): a daemon left behind keeps
# running after the script, and "gradlew --stop" would also kill the daemons of other builds on the machine.
$ErrorActionPreference = 'Stop'
[string[]]$tasks = if ($args.Count -gt 0) { $args } else { @('build') }
if ($tasks -notcontains '--no-daemon') {
    $tasks += '--no-daemon'
}

$loaders = Get-ChildItem -LiteralPath $PSScriptRoot -Directory |
    Where-Object {
        (Test-Path (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path (Join-Path $_.FullName 'gradlew'))
    } |
    Sort-Object Name |
    ForEach-Object { $_.Name }

if (-not $loaders -or $loaders.Count -eq 0) {
    throw "No loader folders discovered (looked for settings.gradle + gradlew under $PSScriptRoot)."
}

foreach ($loader in $loaders) {
    $dir = Join-Path $PSScriptRoot $loader
    Write-Host "==> $loader : gradlew $($tasks -join ' ')"
    Push-Location $dir
    try {
        if ($IsWindows -or $env:OS -eq 'Windows_NT') {
            & .\gradlew.bat @tasks
        } else {
            & ./gradlew @tasks
        }
        if ($LASTEXITCODE -ne 0) {
            throw "$loader build failed (exit code $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}
Write-Host "All loader builds succeeded."

Write-Host "Jars:"
foreach ($loader in $loaders) {
    $libs = Join-Path (Join-Path $PSScriptRoot $loader) 'build/libs'
    if (Test-Path $libs) {
        Get-ChildItem -LiteralPath $libs -Filter '*.jar' |
            Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-dev.jar' } |
            ForEach-Object { Write-Host "  $($_.FullName)" }
    }
}
