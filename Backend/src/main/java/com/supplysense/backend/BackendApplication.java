package com.supplysense.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SupplySense backend entry point.
 *
 * M0 scope: prove the application boots, connects to PostgreSQL,
 * runs Flyway migrations, and exposes Actuator health.
 * No domain modules (auth, catalog, inventory, ...) exist yet -
 * those are introduced module-by-module starting at M1.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
