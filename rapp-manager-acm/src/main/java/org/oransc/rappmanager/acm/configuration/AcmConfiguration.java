package org.oransc.rappmanager.acm.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rappmanager.acm")
@Data
public class AcmConfiguration {

    private String baseUrl;
    private String username;
    private String password;
    private int maxRetries;
    private int retryInterval;
}
