# rAPP ML Optimizer

O-RAN Non-RT RIC rApp that ingests R1 cell performance telemetry, evaluates congestion with an ML/heuristic traffic-steering engine, and publishes declarative A1 policies to the Near-RT RIC A1 Mediator.

Part of the [rApp Starter Kit](../README.md) multi-module Maven project.

## Overview

This rApp implements a reactive pipeline:

1. **R1 Data Consumer** — receives PM telemetry (`CellPerformanceMetrics`) over a WebFlux REST endpoint
2. **Traffic Steering ML Service** — evaluates congestion via a threshold matrix (with an ONNX Runtime hook for future model inference)
3. **A1 Policy Producer** — builds and POSTs O-RAN A1 declarative policies when critical congestion is detected

```text
SMO / R1 Source
      │
      ▼  POST /r1/telemetry
R1DataConsumerController
      │
      ▼
TrafficSteeringMlService  ──►  TrafficSteeringRecommendation
      │                              │
      │ (critical congestion)        ▼
      └──────────────────►  A1PolicyProducerService
                                    │
                                    ▼  POST /a1-policy/v2/policies
                            Near-RT RIC A1 Mediator
```

## Project layout

```text
rAPP ML optimizer/
├── pom.xml
├── src/main/java/com/nokia/powehi/rapp/mloptimizer/
│   ├── MlOptimizerApplication.java          # Spring Boot entry point
│   ├── controller/
│   │   └── R1DataConsumerController.java    # Reactive R1 telemetry endpoint
│   ├── service/
│   │   ├── TrafficSteeringMlService.java    # Congestion evaluation & offload logic
│   │   ├── A1PolicyProducerService.java     # Reactive A1 policy publisher
│   │   ├── OnnxModelHook.java               # Extension point for ONNX Runtime
│   │   └── MockOnnxModelHook.java           # Default mock (defers to heuristics)
│   ├── model/
│   │   ├── CellPerformanceMetrics.java      # R1 PM telemetry DTO
│   │   ├── TrafficSteeringRecommendation.java
│   │   ├── A1PolicyPayload.java
│   │   ├── A1TargetScope.java
│   │   ├── A1DeclarativePolicy.java
│   │   └── TelemetryIngestResponse.java
│   └── config/
│       ├── A1MediatorProperties.java
│       └── WebClientConfig.java
└── src/main/resources/
    └── application.yaml
```

## Prerequisites

- Java 17+
- Maven 3.9+
- Parent `rapp-starter-kit` POM installed or available at `../pom.xml`

## Build

From this directory:

```bash
mvn clean compile
```

From the parent Starter Kit root:

```bash
mvn clean install
```

Build output is written to `%LOCALAPPDATA%\rapp-starter-kit-build\rapp-ml-optimizer\target\` to avoid OneDrive file locks.

## Run

```bash
mvn spring-boot:run
```

The application starts on port **8080** (configurable via `server.port`).

## Configuration

Settings in [`src/main/resources/application.yaml`](src/main/resources/application.yaml):

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP listen port |
| `mloptimizer.a1-mediator.base-url` | `http://localhost:8081` | Near-RT RIC A1 Mediator base URL |
| `mloptimizer.a1-mediator.policies-path` | `/a1-policy/v2/policies` | A1 policy POST path |
| `mloptimizer.a1-mediator.policy-type-id` | `20008` | O-RAN A1 policy type identifier |
| `mloptimizer.a1-mediator.service-id` | `rapp-ml-optimizer` | Owning service / rApp identity |
| `mloptimizer.a1-mediator.status-notification-base` | `http://localhost:8080` | Base URL for policy status callbacks |
| `mloptimizer.a1-mediator.publish-enabled` | `false` | When `false`, policies are mock-accepted without calling the mediator |

Set `publish-enabled: true` to send real HTTP POST requests to the A1 Mediator.

## REST API

### Ingest R1 telemetry

| Method | Path | Status | Description |
|--------|------|--------|-------------|
| `POST` | `/r1/telemetry` | `202 Accepted` | Ingest cell PM metrics and optionally publish an A1 steering policy |

**Query parameters**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `neighborCells` | No | Repeatable list of neighbor cell IDs eligible for traffic offload |

**Request body** — `CellPerformanceMetrics`

```json
{
  "cellId": "cell_nr_mumbai_04",
  "rsrp": -102.5,
  "rsrq": -14.2,
  "activeUsers": 350,
  "prbUtilization": 91.5
}
```

**Response** — `TelemetryIngestResponse`

```json
{
  "cellId": "cell_nr_mumbai_04",
  "criticalCongestion": true,
  "a1PolicyPublished": true,
  "policyId": "ad89020e-75e3-4c36-b749-c27aeb8739e3",
  "recommendation": {
    "sourceCellId": "cell_nr_mumbai_04",
    "criticalCongestion": true,
    "loadBalancingThreshold": 75.0,
    "offloadPercentage": 49.6,
    "targetCellIds": ["cell-102", "cell-103"],
    "rationale": "Critical congestion: PRB 91.5% > 80.0% and RSRQ -14.2 dB < -12.0 dB — recommend offloading 49.6% of active users to neighbor cells"
  }
}
```

## Congestion logic

Critical congestion is declared when **both** conditions are met:

| Metric | Threshold |
|--------|-----------|
| PRB utilization | > **80.0%** |
| RSRQ (average) | < **-12.0 dB** |

When critical, the service computes an offload percentage between **20%** and **50%** using a severity matrix (PRB headroom + RSRQ degradation). An `OnnxModelHook` can override this with model inference; the default `MockOnnxModelHook` defers to the heuristic.

## Example requests

### PowerShell (recommended)

```powershell
Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/r1/telemetry?neighborCells=cell-102&neighborCells=cell-103" `
  -ContentType "application/json" `
  -Body '{"cellId":"cell_nr_mumbai_04","rsrp":-102.5,"rsrq":-14.2,"activeUsers":350,"prbUtilization":91.5}'
```

> **Note:** In PowerShell, `curl` is an alias for `Invoke-WebRequest` and does not support Unix-style `-X`/`-H`/`-d` flags. Use `Invoke-RestMethod` or `curl.exe` with a file/stdin body (see below).

### curl.exe on Windows

Inline JSON with `-d` is unreliable in PowerShell. Use stdin or a file instead:

```powershell
@'
{"cellId":"cell_nr_mumbai_04","rsrp":-102.5,"rsrq":-14.2,"activeUsers":350,"prbUtilization":91.5}
'@ | curl.exe -s --globoff -X POST "http://localhost:8080/r1/telemetry" `
  -H "Content-Type: application/json" -d "@-"
```

### Bash / Linux

```bash
curl -X POST "http://localhost:8080/r1/telemetry?neighborCells=cell-102&neighborCells=cell-103" \
  -H "Content-Type: application/json" \
  -d '{"cellId":"cell_nr_mumbai_04","rsrp":-102.5,"rsrq":-14.2,"activeUsers":350,"prbUtilization":91.5}'
```

## Sample batch payload (10 cells)

A realistic 10-cell telemetry dataset with mixed congestion profiles is in [`samples/telemetry-cells.json`](samples/telemetry-cells.json).

**Critical congestion conditions** (both must be true):

| KPI | Condition | Threshold |
|-----|-----------|-----------|
| `prbUtilization` | greater than | **80.0%** |
| `rsrq` | less than | **-12.0 dB** |

Of the 10 sample cells, **5 are CRITICAL** and **5 are NORMAL** (high PRB-only, poor RSRQ-only, or within limits).

Post all cells using the bundled scripts (the API accepts one cell per request; scripts iterate the array):

```powershell
# Windows (recommended)
.\scripts\post-telemetry.ps1
```

```bash
# Linux / macOS / Git Bash (requires jq)
chmod +x scripts/post-telemetry.sh
./scripts/post-telemetry.sh
```

## Extending ONNX inference

Implement `OnnxModelHook` and register it as a Spring `@Component` (replacing or supplementing `MockOnnxModelHook`):

```java
@Component
public class OnnxRuntimeModelHook implements OnnxModelHook {
    @Override
    public Optional<Double> predictOffloadPercentage(CellPerformanceMetrics metrics) {
        // Load ONNX model, run inference, return offload %
        return Optional.of(predictedValue);
    }
}
```

When the hook returns `Optional.empty()`, the heuristic matrix is used as fallback.

## Tech stack

- Java 17, Spring Boot 3.4.x
- Spring WebFlux (Project Reactor, Netty)
- Lombok, Jakarta Bean Validation
- WebClient for reactive A1 Mediator calls

## Reference

- [O-RAN A1 Interface overview](https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-a1policymanagementservice/en/latest/overview.html)
- [O-RAN R1 Application Protocols (ETSI TS 104 231)](https://www.etsi.org/deliver/etsi_ts/104200_104299/104231/)
- [Parent rApp Starter Kit README](../README.md)
