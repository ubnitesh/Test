package org.oransc.rappmanager.r1.aiml;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.r1.aiml.ModelRelatedInformation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * O-RAN R1 AI/ML model discovery service (ETSI TS 104 231 clause 10.2).
 * Base path: {@code {apiRoot}/ai-ml-model-discovery/v1}
 */
@RestController
@RequestMapping("/ai-ml-model-discovery/v1")
@RequiredArgsConstructor
@Tag(name = "R1 AI/ML Model Discovery")
public class AimlModelDiscoveryController {

    private final AimlModelDiscoveryService aimlModelDiscoveryService;

    @GetMapping("/models")
    @Operation(
            summary = "Discover registered AI/ML models",
            description = "Returns modelId and metadata for registered models. "
                    + "Optional model-name and model-version query parameters are combined with AND.")
    public ResponseEntity<List<ModelRelatedInformation>> discoverModels(
            @RequestParam(name = "model-name", required = false) String modelName,
            @RequestParam(name = "model-version", required = false) String modelVersion) {
        return ResponseEntity.ok(aimlModelDiscoveryService.discoverModels(modelName, modelVersion));
    }
}
