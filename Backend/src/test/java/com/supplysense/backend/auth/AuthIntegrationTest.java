package com.supplysense.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the real HTTP layer end-to-end against a disposable Postgres
 * container (Flyway migrations run for real). This is what proves auth
 * actually works, not just that the mocked unit tests pass.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerThenLoginSucceeds() throws Exception {
        String registerBody = """
                {
                  "businessName": "Acme Retail",
                  "fullName": "Jane Owner",
                  "email": "jane@acme-retail.test",
                  "password": "correct-horse-battery"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.role").value("OWNER"));

        String loginBody = """
                {
                  "email": "jane@acme-retail.test",
                  "password": "correct-horse-battery"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void duplicateEmailRegistrationReturns409() throws Exception {
        String body = """
                {
                  "businessName": "Dupe Co",
                  "fullName": "First User",
                  "email": "dupe@acme.test",
                  "password": "password1234"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String register = """
                {
                  "businessName": "WrongPw Co",
                  "fullName": "User",
                  "email": "wrongpw@acme.test",
                  "password": "correctpassword"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(register))
                .andExpect(status().isCreated());

        String badLogin = """
                {
                  "email": "wrongpw@acme.test",
                  "password": "totally-wrong"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(badLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/some-future-protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
