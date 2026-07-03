package org.oransc.rappmanager.models.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rappmanager.rapps.env")
@Data
public class RappsEnvironmentConfiguration {

    private String smeDiscoveryEndpoint;
}
