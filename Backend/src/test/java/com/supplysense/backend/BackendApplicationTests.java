package com.supplysense.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * M0 smoke test: confirms the Spring application context loads cleanly
 * (datasource connects, Flyway migration runs, beans wire up).
 *
 * This deliberately does NOT use Testcontainers yet - that's introduced
 * in M1 once there's real repository/business logic worth testing against
 * a disposable Postgres instance. For M0, this test expects a Postgres
 * instance to be reachable via the same env vars as application.yml
 * (i.e. run `docker compose up -d postgres` before running this test).
 */
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty. If the context fails to start
        // (bad datasource config, failed migration, misconfigured bean),
        // this test fails - that IS the assertion for M0.
    }
}
