$ErrorActionPreference = 'Stop'
$jdk = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path (Get-Command javac.exe -ErrorAction Stop).Source }
$classes = Join-Path $PSScriptRoot ('build\' + [guid]::NewGuid().ToString('N'))
$gameRoot = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'game-root.txt') -Raw).Trim()
$required = @('Data\scripts.jar', 'Jvm\bin\java.exe', 'Bin\rnr.exe', 'rungame.exe')
foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $gameRoot $relative))) { throw "Game file not found: $(Join-Path $gameRoot $relative)" }
}
$scripts = Join-Path $gameRoot 'Data\scripts.jar'
New-Item -ItemType Directory -Force $classes | Out-Null
& "$jdk\javac.exe" --release 8 -encoding UTF-8 -cp $scripts -d $classes (Get-ChildItem "$PSScriptRoot\src" -Recurse -Filter *.java).FullName
if ($LASTEXITCODE) { throw 'javac failed' }
# Only Java 5 APIs: the embedded VM requires class version 49.
Get-ChildItem $classes -Recurse -Filter *.class | ForEach-Object {
    $bytes = [IO.File]::ReadAllBytes($_.FullName)
    if ($bytes[7] -ne 52) { throw 'Unexpected class version' }
    $bytes[7] = 49
    [IO.File]::WriteAllBytes($_.FullName, $bytes)
}
$manifest = Join-Path $PSScriptRoot 'MANIFEST.MF'
@('Manifest-Version: 1.0', 'Premain-Class: teleport.TeleportAgent', '') | Set-Content $manifest -Encoding ASCII
& "$jdk\jar.exe" cfm "$PSScriptRoot\Teleport.jar" $manifest -C $classes .
if ($LASTEXITCODE) { throw 'jar failed' }
Write-Host "Built $PSScriptRoot\Teleport.jar"
