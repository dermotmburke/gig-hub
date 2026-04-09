package com.d3bot.events;

import org.apache.camel.opentelemetry.starter.CamelOpenTelemetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@CamelOpenTelemetry
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
