package com.nokia.powehi.rapp.mloptimizer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mloptimizer.a1-mediator")
public class A1MediatorProperties {

    private String baseUrl = "http://localhost:8081";

    private String policiesPath = "/a1-policy/v2/policies";

    private String policyTypeId = "20008";

    private String serviceId = "rapp-ml-optimizer";

    private String statusNotificationBase = "http://localhost:8080";

    private boolean publishEnabled = false;
}
