package org.oransc.rappmanager.sme.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rappmanager.sme")
@Data
public class SmeConfiguration {

    private String baseUrl;
    private String providerBasePath;
    private String invokerBasePath;
    private String publishApiBasePath;
    private int maxRetries;
    private int retryInterval;
}
