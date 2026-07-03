package org.oransc.rappmanager.dme.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rappmanager.dme")
@Data
public class DmeConfiguration {

    private String baseUrl;
}
