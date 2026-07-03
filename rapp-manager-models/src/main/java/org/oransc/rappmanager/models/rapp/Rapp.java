package org.oransc.rappmanager.models.rapp;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rapp {

    @NotBlank
    private String name;

    private String version;

    private RappState state;
}
