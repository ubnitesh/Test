package com.nokia.powehi.rapp.mloptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryIngestResponse {

    private String cellId;

    private boolean criticalCongestion;

    private boolean a1PolicyPublished;

    private String policyId;

    private TrafficSteeringRecommendation recommendation;
}
