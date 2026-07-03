Place the packaged Helm chart here before CSAR generation:

`rapp-starter-kit-chart-0.1.0.tgz`

Build the Spring Boot JAR and chart from the project root:

```bash
mvn clean install
helm package <path-to-chart> -d Artifacts/Deployment/HELM
```

Upload the chart to ChartMuseum and update `target_server_uri` in `Definitions/asd.yaml` and
`repository.address` in `Files/Acm/instances/k8s-instance.json`.
