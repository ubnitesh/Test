param(
    [string]$ImageName = "rapp-starter-kit",
    [string]$ImageTag = "0.1.0-SNAPSHOT"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$appDir = Join-Path $root "rapp-manager-application"
$jarSource = Join-Path $env:LOCALAPPDATA "rapp-starter-kit-build\rapp-manager-application\target\rapp-manager-application-0.1.0-SNAPSHOT.jar"
$stagingJar = Join-Path $appDir "app.jar"

if (-not (Test-Path $jarSource)) {
    Write-Host "JAR not found. Building project first..."
    Push-Location $root
    try {
        mvn install -DskipTests -q
    }
    finally {
        Pop-Location
    }
}

if (-not (Test-Path $jarSource)) {
    throw "Expected JAR at: $jarSource"
}

Copy-Item -Path $jarSource -Destination $stagingJar -Force

try {
    docker build -t "${ImageName}:${ImageTag}" --build-arg JAR_FILE=app.jar $appDir
    Write-Host "Image built: ${ImageName}:${ImageTag}"
    Write-Host "Run: docker run -p 8080:8080 ${ImageName}:${ImageTag}"
}
finally {
    if (Test-Path $stagingJar) {
        Remove-Item $stagingJar -Force
    }
}
