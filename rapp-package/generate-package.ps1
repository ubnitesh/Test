param(
    [string]$PackageName = "rapp-starter-kit"
)

$ErrorActionPreference = "Stop"
$sourceDir = Join-Path $PSScriptRoot $PackageName
$outputFile = Join-Path $PSScriptRoot "$PackageName.csar"

if (-not (Test-Path $sourceDir)) {
    throw "Package folder not found: $sourceDir"
}

if (Test-Path $outputFile) {
    Remove-Item $outputFile -Force
}

Push-Location $sourceDir
try {
    Compress-Archive -Path * -DestinationPath $outputFile -Force
    Write-Host "rApp package generated: $outputFile"
}
finally {
    Pop-Location
}
