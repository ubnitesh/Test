package com.nokia.powehi.rapp.mloptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A1DeclarativePolicy {

    private String policyId;

    private String policyTypeId;

    private String serviceId;

    private String statusNotificationUri;

    private A1PolicyPayload policyData;
}
