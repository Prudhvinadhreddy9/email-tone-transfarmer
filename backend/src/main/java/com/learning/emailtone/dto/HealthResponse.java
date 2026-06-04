package com.learning.emailtone.dto;

/**
 * {@code GET /api/health} response.
 *
 * <p>Reports presence of the OpenAI key but NEVER the key itself. Health
 * endpoints are typically unauthenticated, so they must leak zero secret
 * material — not even a partial prefix.
 */
public record HealthResponse(
        String status,
        boolean openAiConfigured,
        String model
) {}
