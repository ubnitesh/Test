package org.oransc.rappmanager.models.r1.aiml;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelId {

    @NotBlank
    private String modelName;

    @NotBlank
    private String modelVersion;

    private String artifactVersion;
}
