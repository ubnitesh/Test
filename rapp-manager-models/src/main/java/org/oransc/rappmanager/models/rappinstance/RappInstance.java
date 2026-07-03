package org.oransc.rappmanager.models.rappinstance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RappInstance {

    @NotBlank
    private String name;

    @NotBlank
    private String rappName;

    private RappInstanceState state;
}
