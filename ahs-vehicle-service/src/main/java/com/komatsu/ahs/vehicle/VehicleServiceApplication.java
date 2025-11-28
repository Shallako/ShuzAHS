package com.komatsu.ahs.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Spring Boot application for AHS Vehicle Service
 * Provides Thrift RPC and REST APIs for vehicle management
 */
@SpringBootApplication
@EnableKafka
public class VehicleServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VehicleServiceApplication.class, args);
    }
}
