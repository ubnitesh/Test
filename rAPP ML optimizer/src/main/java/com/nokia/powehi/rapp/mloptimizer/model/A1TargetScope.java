package com.nokia.powehi.rapp.mloptimizer.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A1TargetScope {

    @NotBlank
    private String cellId;

    private String moi;
}
