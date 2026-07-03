package org.oransc.rappmanager.r1.aiml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.models.r1.aiml.ModelId;
import org.oransc.rappmanager.models.r1.aiml.ModelRelatedInformation;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AimlModelDiscoveryService {

    private final List<ModelRelatedInformation> registeredModels = new CopyOnWriteArrayList<>();

    public AimlModelDiscoveryService() {
        seedSampleModels();
    }

    public List<ModelRelatedInformation> discoverModels(String modelName, String modelVersion) {
        return registeredModels.stream()
                .filter(model -> matches(model, modelName, modelVersion))
                .map(this::copy)
                .toList();
    }

    public void registerModel(ModelRelatedInformation model) {
        registeredModels.removeIf(existing -> sameModelId(existing.getModelId(), model.getModelId()));
        registeredModels.add(copy(model));
        log.info(
                "Registered AI/ML model {}@{}",
                model.getModelId().getModelName(),
                model.getModelId().getModelVersion());
    }

    private boolean matches(ModelRelatedInformation model, String modelName, String modelVersion) {
        boolean nameMatches = Optional.ofNullable(modelName)
                .map(name -> name.equals(model.getModelId().getModelName()))
                .orElse(true);
        boolean versionMatches = Optional.ofNullable(modelVersion)
                .map(version -> version.equals(model.getModelId().getModelVersion()))
                .orElse(true);
        return nameMatches && versionMatches;
    }

    private boolean sameModelId(ModelId left, ModelId right) {
        return left.getModelName().equals(right.getModelName())
                && left.getModelVersion().equals(right.getModelVersion())
                && java.util.Objects.equals(left.getArtifactVersion(), right.getArtifactVersion());
    }

    private ModelRelatedInformation copy(ModelRelatedInformation source) {
        return ModelRelatedInformation.builder()
                .modelId(ModelId.builder()
                        .modelName(source.getModelId().getModelName())
                        .modelVersion(source.getModelId().getModelVersion())
                        .artifactVersion(source.getModelId().getArtifactVersion())
                        .build())
                .metadata(source.getMetadata())
                .build();
    }

    private void seedSampleModels() {
        List<ModelRelatedInformation> samples = new ArrayList<>();
        samples.add(ModelRelatedInformation.builder()
                .modelId(ModelId.builder()
                        .modelName("qos-predictor")
                        .modelVersion("1.0.0")
                        .artifactVersion("1")
                        .build())
                .metadata("{\"author\":\"powehi-rapp\",\"model-type\":\"timeseries\"}")
                .build());
        samples.add(ModelRelatedInformation.builder()
                .modelId(ModelId.builder()
                        .modelName("anomaly-detector")
                        .modelVersion("2.1.0")
                        .build())
                .metadata("{\"author\":\"powehi-rapp\",\"model-type\":\"classification\"}")
                .build());
        registeredModels.addAll(samples);
    }
}
