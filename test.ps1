$ErrorActionPreference = 'Stop'
$gameRoot = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'game-root.txt') -Raw).Trim()
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin'
$classes = Join-Path $PSScriptRoot 'build\tests'
New-Item -ItemType Directory -Force $classes | Out-Null
$cp = "$PSScriptRoot\Teleport.jar;$gameRoot\Data\scripts.jar"
& "$jdk\javac.exe" --release 8 -encoding UTF-8 -cp $cp -d $classes "$PSScriptRoot\test\teleport\DestinationTest.java"
if ($LASTEXITCODE) { throw 'Test compilation failed' }
Get-ChildItem $classes -Recurse -Filter *.class | ForEach-Object {
    $bytes = [IO.File]::ReadAllBytes($_.FullName)
    $bytes[7] = 49
    [IO.File]::WriteAllBytes($_.FullName, $bytes)
}
& "$gameRoot\Jvm\bin\java.exe" -ea -cp "$classes;$cp" teleport.DestinationTest
if ($LASTEXITCODE) { throw 'Test failed' }
