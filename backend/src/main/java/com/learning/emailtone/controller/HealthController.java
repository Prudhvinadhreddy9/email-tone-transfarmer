package com.learning.emailtone.controller;

import com.learning.emailtone.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/health — basic up-check + reports OpenAI configuration state.
 *
 * <p>The reported boolean tells the frontend whether to show a "missing key"
 * banner. The KEY ITSELF is never exposed — health endpoints are typically
 * unauthenticated, so they must leak zero secret material (not even a prefix).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final String apiKey;
    private final String model;

    public HealthController(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        // The "test-" prefix is what we use in the test profile, so we treat it
        // as not-configured for the purpose of the health probe — otherwise CI
        // would falsely report production-ready.
        boolean configured = apiKey != null
                && !apiKey.isBlank()
                && !apiKey.startsWith("test-");
        return new HealthResponse("UP", configured, model);
    }
}
