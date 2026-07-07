# rApp Starter Kit

Spring Boot 17 / Maven multi-module scaffold modeled after the [O-RAN SC Non-RT RIC rApp Manager](https://github.com/o-ran-sc/nonrtric-plt-rappmanager).

## Project layout

```
rapp-starter-kit/
├── pom.xml                          # Parent POM
├── rapp-manager-models/             # Domain models, exceptions, configuration
├── rapp-manager-acm/                # ACM (Automation Composition Management) integration
├── rapp-manager-dme/                # DME (Data Management and Exposure) integration
├── rapp-manager-sme/                # SME (Service Management and Exposure) integration
├── rapp-manager-application/        # Spring Boot application entry point
└── openapi/                         # OpenAPI specs for code generation
```

Package namespace follows O-RAN SC conventions: `org.oransc.rappmanager`.

## Prerequisites

- Java 17+
- Maven 3.9+

## Build

```bash
mvn clean install
```

## Run locally

```bash
cd rapp-manager-application
mvn spring-boot:run <!When you run mvn clean install, you're doing two main things with Maven. First, the "clean" part removes the target folder, which gets rid of any old build files to ensure a fresh start. Then, the "install" part compiles your code, runs any tests, and packages your application into a file, like a JAR, placing it in your local repository so it can be used by other projects.!!>
```

The application starts on port `8080`. Configure external service endpoints in `rapp-manager-application/src/main/resources/application.yaml`:

| Property | Description |
|----------|-------------|
| `rappmanager.acm.baseurl` | ONAP Policy CLAMP ACM endpoint |
| `rappmanager.sme.baseurl` | Service Management and Exposure endpoint |
| `rappmanager.dme.baseurl` | Information Service / DME endpoint |
| `rappmanager.rapps.env.smeDiscoveryEndpoint` | SME service API discovery |

## REST API (starter)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/rapps` | List rApps |
| POST | `/rapps` | Create rApp |
| GET | `/rapps/{name}` | Get rApp |
| DELETE | `/rapps/{name}` | Delete rApp |
| GET | `/rapps/{name}/instances` | List instances |
| POST | `/rapps/{name}/instances` | Create instance |
| GET | `/rapps/{name}/instances/{instance}` | Get instance |
| DELETE | `/rapps/{name}/instances/{instance}` | Delete instance |

## R1 interface REST APIs

Aligned with [O-RAN R1 Application Protocols](https://www.etsi.org/deliver/etsi_ts/104200_104299/104231/) (ETSI TS 104 231).

### Configuration Management (`ran-oam-cm/v1`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ran-oam-cm/v1/{moiPath}` | Read MOI configuration data |
| PATCH | `/ran-oam-cm/v1/{moiPath}` | Write configuration changes |

Example:

```bash
curl http://localhost:8080/ran-oam-cm/v1/SubNetwork=SN1,GNBDUFunction=1
```

### AI/ML model discovery (`ai-ml-model-discovery/v1`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ai-ml-model-discovery/v1/models` | Discover registered AI/ML models |
| GET | `/ai-ml-model-discovery/v1/models?model-name={name}&model-version={ver}` | Filtered discovery (AND) |

OpenAPI specs: `openapi/r1/configuration-management-api.yaml`, `openapi/r1/ai-ml-model-discovery-api.yaml`

## Docker

Maven builds the JAR under `%LOCALAPPDATA%\rapp-starter-kit-build\` (see parent `pom.xml`).
Start **Docker Desktop**, then from the project root:

```powershell
.\docker-build.ps1
docker run -p 8080:8080 rapp-starter-kit:0.1.0-SNAPSHOT
```

Or run without Docker:

```powershell
cd rapp-manager-application
mvn spring-boot:run
```

## Reference

- [O-RAN SC rApp Manager documentation](https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappmanager/en/latest/)
- [O-RAN SC rApp Manager source](https://github.com/o-ran-sc/nonrtric-plt-rappmanager)
