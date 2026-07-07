package org.oransc.rappmanager.dme.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(prefix = "rappmanager.dme.telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DmeTelemetryDataSourceConfiguration {

    @Bean
    public DataSource dmeTelemetryDataSource(DmeConfiguration dmeConfiguration) {
        Path databasePath = resolveDatabasePath(dmeConfiguration.getTelemetry().getDatabasePath());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
        return dataSource;
    }

    @Bean
    public JdbcTemplate dmeTelemetryJdbcTemplate(DataSource dmeTelemetryDataSource) {
        return new JdbcTemplate(dmeTelemetryDataSource);
    }

    static Path resolveDatabasePath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        return path;
    }
}
