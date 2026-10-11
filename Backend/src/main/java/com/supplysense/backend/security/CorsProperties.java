package com.supplysense.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Added for real deployment: the allowed frontend origin can no longer
 * be a hardcoded "http://localhost:5173" once there's a real frontend
 * URL (Vercel) calling a real backend URL (Render). Comma-separated so
 * both a local dev origin and a production origin can be allowed at
 * once during the transition, without a code change.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
