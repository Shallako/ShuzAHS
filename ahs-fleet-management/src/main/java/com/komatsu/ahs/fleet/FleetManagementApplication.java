package com.komatsu.ahs.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Fleet Management Application
 * 
 * Manages the autonomous haulage fleet operations including:
 * - Real-time fleet monitoring and control
 * - Route optimization and assignment
 * - Load/dump location management
 * - Integration with DISPATCH Fleet Management System
 * 
 * @author Komatsu MTS Team
 */
@SpringBootApplication(scanBasePackages = {
    "com.komatsu.ahs.fleet",
    "com.komatsu.ahs.common"
})
@EnableKafka
@EnableCaching
@EnableScheduling
public class FleetManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetManagementApplication.class, args);
    }
}
