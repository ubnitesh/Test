package org.oransc.rappmanager.models.r1.cm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationModificationRequest {

    @NotEmpty
    @Valid
    private List<MoModification> modifications;
}
