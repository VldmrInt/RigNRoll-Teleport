$ErrorActionPreference = 'Stop'
$gameRoot = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'game-root.txt') -Raw).Trim()
$gameExe = Join-Path $gameRoot 'Bin\rnr.exe'
$launcher = Join-Path $gameRoot 'rungame.exe'
$agent = Join-Path $PSScriptRoot 'Teleport.jar'
$agentOptions = ''

$running = Get-Process rnr -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $gameExe }
if ($running) {
    throw "Rig'n'Roll is already running (PID $($running.Id)). Close it, then start this script."
}
if (-not (Test-Path -LiteralPath $agent)) {
    & (Join-Path $PSScriptRoot 'build.ps1')
}

$previousTools = $env:JAVA_TOOL_OPTIONS
$agentOption = '-javaagent:"' + $agent + '"=' + $agentOptions
$env:JAVA_TOOL_OPTIONS = if ([string]::IsNullOrWhiteSpace($previousTools)) { $agentOption } else { $previousTools + ' ' + $agentOption }
Write-Host 'Teleport: use the Teleport window after loading a game (Alt+Tab).'
Push-Location $gameRoot
try {
    & $launcher
} finally {
    Pop-Location
    $env:JAVA_TOOL_OPTIONS = $previousTools
}
