param([string]$MinecraftDirectory = "$env:APPDATA\.minecraft", [switch]$Test, [switch]$Regression)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$cache = Join-Path $project '.cache'
$classes = Join-Path $project 'build\classes\main'
$testClasses = Join-Path $project 'build\classes\test'
$libs = Join-Path $project 'build\libs'
$version = (Get-Content -LiteralPath (Join-Path $project 'src\main\resources\fabric.mod.json') -Raw -Encoding UTF8 | ConvertFrom-Json).version
New-Item -ItemType Directory -Force -Path $cache,$classes,$libs | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$api = Get-ChildItem -LiteralPath (Join-Path $MinecraftDirectory 'mods') -Filter 'fabric-api-*+26.3.jar' | Select-Object -First 1
if (-not $api) { throw 'Minecraft 26.3 Fabric API jar not found in mods.' }
$apiDir = Join-Path $cache $api.BaseName
if (-not (Test-Path -LiteralPath $apiDir)) { [System.IO.Compression.ZipFile]::ExtractToDirectory($api.FullName, $apiDir) }
$game = Join-Path $MinecraftDirectory 'versions\26.3\26.3.jar'
$manifest = Get-Content -LiteralPath (Join-Path $MinecraftDirectory 'versions\26.3\26.3.json') -Raw | ConvertFrom-Json
$profile = Get-Content -LiteralPath (Join-Path $MinecraftDirectory 'versions\fabric-loader-0.19.5-26.3\fabric-loader-0.19.5-26.3.json') -Raw | ConvertFrom-Json
$libraryPaths = @($manifest.libraries | ForEach-Object { if ($_.downloads.artifact.path) { Join-Path (Join-Path $MinecraftDirectory 'libraries') $_.downloads.artifact.path } })
foreach ($library in $profile.libraries) {
    $parts = $library.name.Split(':')
    $libraryPaths += Join-Path (Join-Path $MinecraftDirectory 'libraries') ($parts[0].Replace('.','/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar')
}
$dependencies = @($game) + @($libraryPaths | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -Unique) + @(Get-ChildItem -LiteralPath $apiDir -Recurse -Filter '*.jar' | ForEach-Object FullName)
$cp = $dependencies -join ';'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $project 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
if ($Regression) { $sources += Join-Path $project 'src\test\java\dev\tagmod\MonsterTraitRegression.java' }
$arguments = @('-encoding','UTF-8','--release','25','-proc:none','-classpath',$cp,'-d',$classes) + $sources
$argFile = Join-Path $cache 'javac.args'
$argLines = @($arguments | ForEach-Object { '"' + $_.Replace('\','/').Replace('"','\"') + '"' })
[System.IO.File]::WriteAllLines($argFile, $argLines, (New-Object System.Text.UTF8Encoding($false)))
& javac "@$argFile"
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
if ($Regression) {
    $regressionPackage = Join-Path $testClasses 'dev\tagmod'
    New-Item -ItemType Directory -Force -Path $regressionPackage | Out-Null
    Move-Item -LiteralPath (Join-Path $classes 'dev\tagmod\MonsterTraitRegression.class') -Destination (Join-Path $regressionPackage 'MonsterTraitRegression.class') -Force
}
Copy-Item -Path (Join-Path $project 'src\main\resources\*') -Destination $classes -Recurse -Force
& jar "-J-Djava.io.tmpdir=$cache" --create --file (Join-Path $libs "tagmod-$version.jar") -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
$cp | Set-Content -LiteralPath (Join-Path $cache 'classpath.txt') -Encoding utf8
if ($Regression) {
    & java -classpath "$testClasses;$classes;$cp" dev.tagmod.MonsterTraitRegression
    if ($LASTEXITCODE -ne 0) { throw 'Monster trait regression failed.' }
}
if ($Test) {
    $testSources = @(Get-ChildItem -LiteralPath (Join-Path $project 'src\test\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
    New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
    & javac -encoding UTF-8 --release 25 -proc:none -classpath "$classes;$cp" -d $testClasses @testSources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java -ea -classpath "$testClasses;$classes;$cp" dev.tagmod.CoreTests
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
}
Write-Host "Built: $libs\tagmod-$version.jar"
