package com.monitoring.controller;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    @Autowired
    private MeterRegistry meterRegistry;

    @GetMapping("/")
    public String home() {
        return "Java Monitoring App";
    }

    @GetMapping("/users")
    public String users() {
        return "Users Endpoint";
    }

    @GetMapping("/orders")
    public String orders() {

        meterRegistry.counter("orders_count").increment();

        return "Order Created";
    }
}
