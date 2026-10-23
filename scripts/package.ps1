$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$version = (Get-Content -LiteralPath (Join-Path $project 'src\main\resources\fabric.mod.json') -Raw -Encoding UTF8 | ConvertFrom-Json).version
$destination = Join-Path $project "dist\TagMod-$version"
New-Item -ItemType Directory -Force -Path (Join-Path $destination 'mods'),(Join-Path $destination 'config') | Out-Null
Copy-Item -LiteralPath (Join-Path $project "build\libs\tagmod-$version.jar") -Destination (Join-Path $destination 'mods') -Force
Copy-Item -LiteralPath (Join-Path $project 'config\tagmod.json') -Destination (Join-Path $destination 'config') -Force
Copy-Item -LiteralPath (Join-Path $project 'README.md'),(Join-Path $project 'LICENSE') -Destination $destination -Force
Copy-Item -LiteralPath (Join-Path $project 'docs') -Destination $destination -Recurse -Force
Compress-Archive -Path (Join-Path $destination '*') -DestinationPath (Join-Path $project "dist\TagMod-$version.zip") -Force
Write-Host "Packaged: $project\dist\TagMod-$version.zip"
