package org.oransc.rappmanager.models.r1.aiml;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRelatedInformation {

    @NotNull
    @Valid
    private ModelId modelId;

    @NotBlank
    private String metadata;
}
