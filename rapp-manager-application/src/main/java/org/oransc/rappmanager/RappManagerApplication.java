package org.oransc.rappmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "org.oransc.rappmanager")
@EnableConfigurationProperties
@EnableCaching
public class RappManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RappManagerApplication.class, args);
    }
}
