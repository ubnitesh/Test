package com.nokia.powehi.rapp.mloptimizer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A1PolicyPayload {

    @NotNull
    @Valid
    private A1TargetScope targetScope;

    @NotEmpty
    private String[] policyStatements;
}
