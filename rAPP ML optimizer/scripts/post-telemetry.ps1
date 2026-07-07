# Post each cell from samples/telemetry-cells.json to the rAPP ML Optimizer R1 telemetry endpoint.
#
# Critical congestion is declared when BOTH conditions are met:
#   - prbUtilization > 80.0%
#   - rsrq           < -12.0 dB
#
# Usage:
#   .\scripts\post-telemetry.ps1
#   .\scripts\post-telemetry.ps1 -BaseUrl "http://localhost:8080"
#   .\scripts\post-telemetry.ps1 -JsonFile "samples\telemetry-cells.json"

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$JsonFile = (Join-Path $PSScriptRoot "..\samples\telemetry-cells.json")
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $JsonFile)) {
    Write-Error "JSON file not found: $JsonFile"
}

$data = Get-Content $JsonFile -Raw | ConvertFrom-Json

Write-Host "=== Critical congestion conditions ===" -ForegroundColor Cyan
Write-Host $data.criticalCongestionConditions.description
foreach ($rule in $data.criticalCongestionConditions.rules) {
    Write-Host ("  {0} {1} {2} {3} — {4}" -f $rule.kpi, $rule.operator, $rule.threshold, $rule.unit, $rule.notes)
}
Write-Host ""

$neighborQuery = ($data.neighborCells | ForEach-Object { "neighborCells=$_" }) -join "&"
$endpoint = "$BaseUrl/r1/telemetry"
if ($neighborQuery) {
    $endpoint = "$endpoint?$neighborQuery"
}

$cells = $data.cells
Write-Host "Posting $($cells.Count) cells to $endpoint"
Write-Host ""

$index = 0
foreach ($cell in $cells) {
    $index++
    $expected = if ($cell.expectedCongestion) { $cell.expectedCongestion } else { "UNKNOWN" }
    Write-Host "--- [$index/$($cells.Count)] $($cell.cellId) (expected: $expected) ---" -ForegroundColor Yellow

    $payload = @{
        cellId          = $cell.cellId
        rsrp            = $cell.rsrp
        rsrq            = $cell.rsrq
        activeUsers     = $cell.activeUsers
        prbUtilization  = $cell.prbUtilization
    } | ConvertTo-Json -Compress

    try {
        $response = Invoke-RestMethod -Method POST -Uri $endpoint -ContentType "application/json" -Body $payload
        $policyId = if ($response.policyId) { $response.policyId } else { "none" }
        $offload = if ($response.recommendation.offloadPercentage) { $response.recommendation.offloadPercentage } else { 0 }
        Write-Host ("  HTTP 202 | criticalCongestion={0} | offload={1}% | policyId={2}" -f `
            $response.criticalCongestion, [math]::Round($offload, 1), $policyId) -ForegroundColor Green
    }
    catch {
        Write-Host "  HTTP ERROR | $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "Done." -ForegroundColor Cyan
