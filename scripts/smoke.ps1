<#
.SYNOPSIS
	Runs the in-game smoke test (docs/TESTING.md) of one or more loaders and checks the outcome.

.DESCRIPTION
	For every requested loader folder that exists (fabric, neoforge, forge) this runs `gradlew runSmoke`, which launches a
	dev client, creates a test world, takes six screenshots of the mod and exits. Afterwards the script checks:
	  - the Gradle exit code,
	  - run-smoke/smoke-result.json: every step ok, all six screenshots present,
	  - the six PNGs: valid, not blank (distinct colours) and with pixels of the outline colour,
	  - run-smoke/logs/latest.log: no ERROR/Exception line that mentions glintsmith or navrelis, no WARN line of Glintsmith
	    (this includes the outline hook self-check warning "Some outline hooks were not applied"), no mixin apply failure.
	Screenshots, result and log are copied to <OutDir>/<minecraft>-<loader>/. A PASS/FAIL table is printed; the exit code is 0
	only if every loader passed.

	A loader only needs a `runSmoke` Gradle task that writes <loader>/run-smoke/smoke-result.json (see docs/TESTING.md).

.PARAMETER Loader
	fabric, neoforge, forge; one or several, e.g. -Loader fabric,neoforge. Default: fabric.

.PARAMETER OutDir
	Where the results are collected. Default: build/smoke in the repository root.

.PARAMETER TimeoutMinutes
	Per loader; the game has its own watchdog, this one also covers a Gradle that hangs. Default 10.

.EXAMPLE
	pwsh scripts/smoke.ps1 -Loader fabric
#>
[CmdletBinding()]
param(
	[string[]]$Loader = @('fabric'),
	[string]$OutDir,
	[int]$TimeoutMinutes = 10
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutDir) {
	$OutDir = Join-Path $root 'build/smoke'
}
$OutDir = [System.IO.Path]::GetFullPath($OutDir)
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# "-Loader fabric,neoforge" arrives as one string when the script is started with pwsh -File
$loaders = @($Loader | ForEach-Object { $_ -split '[,\s]+' } | Where-Object { $_ } | ForEach-Object { $_.ToLowerInvariant() })

$shots = '01_first_person', '02_inventory', '03_third_person_front', '04_glintsmith_screen', '05_dropped_closeup', '06_item_frame_closeup'
$minDistinctColors = 200
$minOutlinePixels = 50

function New-Check([string]$Name, [bool]$Ok, [string]$Detail = '') {
	[pscustomobject]@{ Name = $Name; Ok = $Ok; Detail = $Detail }
}

function Get-MinecraftVersion {
	$props = Join-Path $root 'gradle/shared.properties'
	if (Test-Path $props) {
		$line = Select-String -Path $props -Pattern '^\s*minecraft_version\s*=\s*(\S+)' | Select-Object -First 1
		if ($line) { return $line.Matches[0].Groups[1].Value }
	}
	return 'unknown'
}

function Test-Loader([string]$Name) {
	$dir = Join-Path $root $Name
	$checks = [System.Collections.Generic.List[object]]::new()
	$mc = Get-MinecraftVersion
	$runDir = Join-Path $dir 'run-smoke'
	$resultFile = Join-Path $runDir 'smoke-result.json'
	$stamp = [System.Diagnostics.Stopwatch]::StartNew()

	if (-not (Test-Path (Join-Path $dir 'gradlew.bat'))) {
		$checks.Add((New-Check 'loader folder' $false "$dir has no gradlew.bat"))
		return [pscustomobject]@{ Loader = $Name; Minecraft = $mc; Checks = $checks; Seconds = 0; Target = $null }
	}

	# --- run -----------------------------------------------------------------------------------------------------------
	$gradleLog = Join-Path $OutDir "$Name-gradle.log"
	Write-Host "[$Name] gradlew runSmoke ..."
	$p = Start-Process -FilePath (Join-Path $dir 'gradlew.bat') -ArgumentList @('runSmoke', '--console=plain') `
		-WorkingDirectory $dir -PassThru -NoNewWindow -RedirectStandardOutput $gradleLog -RedirectStandardError "$gradleLog.err"
	$null = $p.Handle # keeps the exit code readable after WaitForExit
	if (-not $p.WaitForExit($TimeoutMinutes * 60000)) {
		& taskkill.exe /PID $p.Id /T /F | Out-Null
		$checks.Add((New-Check 'gradle runSmoke' $false "no exit after $TimeoutMinutes minutes (killed; a game process may still be running)"))
	} else {
		$checks.Add((New-Check 'gradle runSmoke' ($p.ExitCode -eq 0) "exit code $($p.ExitCode)"))
	}
	$elapsed = [int]$stamp.Elapsed.TotalSeconds

	# --- result file ---------------------------------------------------------------------------------------------------
	$result = $null
	if (Test-Path $resultFile) {
		try { $result = Get-Content -Raw -Path $resultFile | ConvertFrom-Json } catch { }
	}
	if (-not $result) {
		$checks.Add((New-Check 'smoke-result.json' $false 'missing or not valid JSON: the game did not finish'))
	} else {
		if ($result.minecraft -and $result.minecraft -ne 'unknown') { $mc = $result.minecraft }
		$bad = @($result.steps | Where-Object { -not $_.ok } | ForEach-Object { "$($_.name): $($_.error)" })
		if ($result.fatal) { $bad += "fatal: $($result.fatal)" }
		$checks.Add((New-Check 'steps' ($bad.Count -eq 0 -and $result.passed) $(if ($bad.Count) { $bad -join '; ' } else { "$(@($result.steps).Count) steps ok in $([int]($result.durationMs / 1000)) s" })))
		$checks.Add((New-Check 'config' ($result.config -and -not $result.config.lastError -and @($result.config.warnings).Count -eq 0) `
			$(if ($result.config) { "thickness $($result.config.outlineThickness), item glint $($result.config.glintItemTexture)" } else { 'not recorded' })))
	}

	# --- screenshots ---------------------------------------------------------------------------------------------------
	$shotProblems = @()
	$shotInfo = @()
	foreach ($shot in $shots) {
		$file = Join-Path $runDir "screenshots/$shot.png"
		$step = if ($result) { @($result.steps | Where-Object { $_.name -eq $shot })[0] } else { $null }
		if (-not (Test-Path $file)) { $shotProblems += "$shot missing"; continue }
		$bytes = [System.IO.File]::ReadAllBytes($file)
		if ($bytes.Length -lt 5000 -or $bytes[0] -ne 0x89 -or $bytes[1] -ne 0x50 -or $bytes[2] -ne 0x4E -or $bytes[3] -ne 0x47) {
			$shotProblems += "$shot is not a valid PNG ($($bytes.Length) bytes)"; continue
		}
		if (-not $step -or $null -eq $step.distinctColors) { $shotProblems += "$shot has no statistics in the result"; continue }
		if ($step.distinctColors -lt $minDistinctColors) { $shotProblems += "$shot looks blank ($($step.distinctColors) colours)" }
		if ($step.outlinePixels -lt $minOutlinePixels) { $shotProblems += "$shot has no outline ($($step.outlinePixels) px of the outline colour)" }
		$shotInfo += "$($shot.Substring(0, 2)):$($step.outlinePixels)px"
	}
	$checks.Add((New-Check 'screenshots' ($shotProblems.Count -eq 0) $(if ($shotProblems.Count) { $shotProblems -join '; ' } else { "6 PNGs, outline pixels $($shotInfo -join ' ')" })))

	# --- log -----------------------------------------------------------------------------------------------------------
	$logFile = Join-Path $runDir 'logs/latest.log'
	if (-not (Test-Path $logFile)) {
		$checks.Add((New-Check 'log' $false "$logFile missing"))
	} else {
		$lines = Get-Content -Path $logFile
		$errors = @($lines | Where-Object { $_ -match '(\bERROR\b|Exception)' -and $_ -match '(?i)(glintsmith|navrelis)' })
		$checks.Add((New-Check 'log: errors' ($errors.Count -eq 0) $(if ($errors.Count) { "$($errors.Count) line(s), first: $($errors[0])" } else { 'no ERROR/Exception line mentions glintsmith' })))

		$hookWarn = @($lines | Where-Object { $_ -match 'Some outline hooks were not applied' })
		$checks.Add((New-Check 'log: outline hook self-check' ($hookWarn.Count -eq 0) $(if ($hookWarn.Count) { $hookWarn[0] } else { 'all outline hooks applied (no warning)' })))

		$warns = @($lines | Where-Object { $_ -match '\bWARN\b' -and $_ -match '(?i)glintsmith' -and $_ -notmatch 'Some outline hooks were not applied' })
		$checks.Add((New-Check 'log: warnings' ($warns.Count -eq 0) $(if ($warns.Count) { "$($warns.Count) line(s), first: $($warns[0])" } else { 'no Glintsmith warning' })))

		$mixin = @($lines | Where-Object { $_ -match '(?i)(Mixin apply for mod|Mixin transformation of|InvalidInjectionException|MixinTransformerError|MixinApplyError|Critical injection failure|InjectionError|Mixin .* failed|@Mixin target .* was not found)' })
		$checks.Add((New-Check 'log: mixins' ($mixin.Count -eq 0) $(if ($mixin.Count) { "$($mixin.Count) line(s), first: $($mixin[0])" } else { 'no mixin apply failure' })))
	}

	# --- collect -------------------------------------------------------------------------------------------------------
	$target = Join-Path $OutDir "$mc-$Name"
	if (Test-Path $target) { Remove-Item -Recurse -Force $target }
	New-Item -ItemType Directory -Force -Path $target | Out-Null
	if (Test-Path (Join-Path $runDir 'screenshots')) { Copy-Item -Path (Join-Path $runDir 'screenshots/*') -Destination $target }
	foreach ($f in @($resultFile, $logFile, $gradleLog)) {
		if (Test-Path $f) { Copy-Item -Path $f -Destination $target }
	}
	return [pscustomobject]@{ Loader = $Name; Minecraft = $mc; Checks = $checks; Seconds = $elapsed; Target = $target }
}

$reports = @()
foreach ($name in $loaders) {
	if ($name -notin 'fabric', 'neoforge', 'forge') {
		Write-Warning "unknown loader '$name' (fabric, neoforge, forge)"
		continue
	}
	if (-not (Test-Path (Join-Path $root $name))) {
		Write-Host "[$name] no folder in the repository, skipped"
		continue
	}
	$reports += Test-Loader $name
}

Write-Host ''
$allOk = $reports.Count -gt 0
foreach ($r in $reports) {
	$ok = -not @($r.Checks | Where-Object { -not $_.Ok }).Count
	$allOk = $allOk -and $ok
	Write-Host ("{0,-9} {1,-9} {2,-5} {3,4} s   {4}" -f $r.Loader, $r.Minecraft, $(if ($ok) { 'PASS' } else { 'FAIL' }), $r.Seconds, $r.Target)
	foreach ($c in $r.Checks) {
		Write-Host ("    {0,-4} {1,-30} {2}" -f $(if ($c.Ok) { 'ok' } else { 'FAIL' }), $c.Name, $c.Detail)
	}
}
if (-not $reports.Count) {
	Write-Host 'nothing was run'
}
exit $(if ($allOk) { 0 } else { 1 })
