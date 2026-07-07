package com.nokia.powehi.rapp.mloptimizer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficSteeringRecommendation {

    private String sourceCellId;

    private boolean criticalCongestion;

    private double loadBalancingThreshold;

    private double offloadPercentage;

    private List<String> targetCellIds;

    private String rationale;
}
