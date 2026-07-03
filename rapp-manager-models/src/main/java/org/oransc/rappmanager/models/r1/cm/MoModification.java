package org.oransc.rappmanager.models.r1.cm;

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
public class MoModification {

    @NotNull
    private ModifyOperator modifyOperator;

    @NotBlank
    private String path;

    private Object value;
}
