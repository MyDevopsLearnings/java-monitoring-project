package com.monitoring.java_monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.monitoring")
public class JavaMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaMonitoringApplication.class, args);
    }
}
