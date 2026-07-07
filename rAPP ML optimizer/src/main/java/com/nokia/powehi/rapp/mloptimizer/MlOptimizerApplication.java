package com.nokia.powehi.rapp.mloptimizer;

import com.nokia.powehi.rapp.mloptimizer.config.A1MediatorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(A1MediatorProperties.class)
public class MlOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlOptimizerApplication.class, args);
    }
}
