package org.oransc.rappmanager.dme.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rappmanager.dme")
@Data
public class DmeConfiguration {

    private String baseUrl;
    private Telemetry telemetry = new Telemetry();

    @Data
    public static class Telemetry {

        /**
         * When false, DME telemetry REST endpoints are not registered.
         */
        private boolean enabled = true;

        /**
         * SQLite database produced by ran_cell_simulator.py.
         */
        private String databasePath = "../rapp-manager-models/data/ran_telemetry.db";

        /**
         * HTTP base path for KPI exposure (appended to server context).
         */
        private String apiBasePath = "/dme/ran-telemetry/v1";
    }
}
