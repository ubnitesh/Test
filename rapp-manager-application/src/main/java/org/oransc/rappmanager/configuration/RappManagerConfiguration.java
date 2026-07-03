package org.oransc.rappmanager.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rappmanager")
@Data
public class RappManagerConfiguration {

    private String csarLocation;
}
