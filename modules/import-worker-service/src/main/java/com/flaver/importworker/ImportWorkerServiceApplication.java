package com.flaver.importworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImportWorkerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImportWorkerServiceApplication.class, args);
    }
}
