param(
    [string]$PackageName = "rapp-starter-kit"
)

$ErrorActionPreference = "Stop"
$sourceDir = Join-Path $PSScriptRoot $PackageName
$outputFile = Join-Path $PSScriptRoot "$PackageName.csar"
$zipFile = Join-Path $PSScriptRoot "$PackageName.zip"

if (-not (Test-Path $sourceDir)) {
    throw "Package folder not found: $sourceDir"
}

foreach ($file in @($outputFile, $zipFile)) {
    if (Test-Path $file) {
        Remove-Item $file -Force
    }
}

Push-Location $sourceDir
try {
    # CSAR is a ZIP archive; Compress-Archive only accepts .zip extension
    Compress-Archive -Path * -DestinationPath $zipFile -Force
}
finally {
    Pop-Location
}

Rename-Item -Path $zipFile -NewName (Split-Path $outputFile -Leaf)
Write-Host "rApp package generated: $outputFile"
