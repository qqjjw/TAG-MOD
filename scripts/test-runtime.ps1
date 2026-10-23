param([ValidateSet('SERVER','CLIENT')][string]$Environment = 'SERVER', [string]$MinecraftDirectory = "$env:APPDATA\.minecraft")
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
Push-Location $project
try {
    $runtimeCp = (Get-Content -LiteralPath '.cache\classpath.txt' -Raw).Trim()
    $testDir = (Resolve-Path 'build\classes\test').Path
    $version = (Get-Content -LiteralPath 'src\main\resources\fabric.mod.json' -Raw -Encoding UTF8 | ConvertFrom-Json).version
    $modJar = (Resolve-Path "build\libs\tagmod-$version.jar").Path
    $apiJar = (Get-ChildItem -LiteralPath (Join-Path $MinecraftDirectory 'mods') -Filter 'fabric-api-*+26.3.jar' | Select-Object -First 1).FullName
    New-Item -ItemType Directory -Force -Path '.cache\runtime-mods' | Out-Null
    $ErrorActionPreference = 'Continue'
    $output = & java -ea '-Dtagmod.mixinTests=true' "-Dtagmod.testEnvironment=$Environment" "-Dfabric.gameJarPath=$MinecraftDirectory\versions\26.3\26.3.jar" "-Dfabric.addMods=$modJar;$apiJar" '-Dfabric.modsFolder=.cache/runtime-mods' -classpath "$testDir;$runtimeCp" dev.tagmod.RuntimeHarness 2>&1
    $result = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $output | Out-File -LiteralPath ".cache\runtime-$Environment.log" -Encoding utf8
    if ($result -ne 0) { $output | Select-Object -Last 65; throw "Fabric $Environment tests failed; see .cache/runtime-$Environment.log" }
    $output | Select-String -Pattern 'TAGMOD TESTS PASSED|ERROR|Mixin apply'
} finally { Pop-Location }
