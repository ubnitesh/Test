# rApp Package (ASD / CSAR)

O-RAN SC rApp Manager onboarding package for the Spring Boot **rapp-starter-kit** application.

## Directory layout

```
rapp-starter-kit/
├── asd.mf                              # CSAR manifest metadata (YAML)
├── TOSCA-Metadata/TOSCA.meta           # TOSCA entry point
├── Definitions/
│   ├── asd.yaml                        # ASD topology + Helm deployment item
│   └── asd_types.yaml                  # TOSCA ASD types
├── metadata/
│   ├── rapp-package-metadata.json      # Canonical JSON package descriptor
│   └── spring-boot-deployment.json     # Spring Boot / K8s deployment metadata
├── Artifacts/Deployment/HELM/          # Helm chart .tgz (add before packaging)
├── Files/
│   ├── Acm/
│   │   ├── definition/compositions.json
│   │   └── instances/k8s-instance.json
│   └── Sme/
│       ├── providers/provider-function-1.json
│       ├── serviceapis/rapp-lifecycle-api.json
│       ├── serviceapis/r1-interface-api.json
│       └── invokers/invoker-rapp-consumer.json
```

## JSON metadata files

| File | Purpose |
|------|---------|
| `metadata/rapp-package-metadata.json` | Top-level ASD descriptor, deployment items, package layout |
| `metadata/spring-boot-deployment.json` | Spring Boot image, Helm, probes, env overrides |
| `Files/Acm/definition/compositions.json` | ONAP ACM TOSCA composition for K8s microservice |
| `Files/Acm/instances/k8s-instance.json` | ACM instance with Helm chart overrides |
| `Files/Sme/providers/provider-function-1.json` | SME provider (APF/AEF) registration |
| `Files/Sme/serviceapis/*.json` | Published REST API profiles |
| `Files/Sme/invokers/invoker-rapp-consumer.json` | API invoker registration |

## Build CSAR

1. Build the Spring Boot JAR: `mvn clean install`
2. Package the Helm chart into `Artifacts/Deployment/HELM/`
3. Update ChartMuseum URIs in `Definitions/asd.yaml` and `k8s-instance.json`
4. Generate CSAR:

```powershell
.\generate-package.ps1
```

## Onboard with rApp Manager

Upload `rapp-starter-kit.csar` to the rApp Manager CSAR location and create an rApp instance.
Placeholder tokens such as `DO_NOT_CHANGE_THIS_COMPOSITION_ID` are replaced by rApp Manager at runtime.
