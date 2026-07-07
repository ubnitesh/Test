package com.nokia.powehi.rapp.mloptimizer.model;

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
public class CellPerformanceMetrics {

    @NotBlank
    private String cellId;

    @NotNull
    private Double rsrp;

    @NotNull
    private Double rsrq;

    @NotNull
    private Integer activeUsers;

    @NotNull
    private Double prbUtilization;
}
