package com.learning.emailtone.dto;

import com.learning.emailtone.model.Tone;

import java.util.List;
import java.util.UUID;

/**
 * Outbound response body for {@code POST /api/transform}.
 *
 * <p>Distinct from {@code LlmTransformOutput} (the LLM's narrower output
 * shape). The service combines the LLM output with server-supplied fields
 * (transformId, original echo, token usage) before returning this DTO.
 */
public record TransformResponse(
        UUID transformId,
        String originalEmail,
        String transformedEmail,
        Tone tone,
        List<String> changesNotes,
        TokenUsageDto tokenUsage
) {}
