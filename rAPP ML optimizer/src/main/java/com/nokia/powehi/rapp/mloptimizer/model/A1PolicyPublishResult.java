package com.nokia.powehi.rapp.mloptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A1PolicyPublishResult {

    private String policyId;

    private String status;
}
