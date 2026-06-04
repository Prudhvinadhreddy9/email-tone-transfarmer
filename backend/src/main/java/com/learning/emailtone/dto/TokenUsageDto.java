package com.learning.emailtone.dto;

/**
 * Token accounting attached to every transform response.
 *
 * <p>WHY surface this to the client: the consumer is paying for every call
 * (pass-through cost). Showing per-call cost makes the user aware of usage
 * and is useful in the UI for setting expectations.
 *
 * @param estimatedCostUsd computed server-side from hardcoded gpt-4o-mini
 *                         prices. "Estimated" because token prices change and
 *                         we don't auto-update the constants.
 */
public record TokenUsageDto(
        int promptTokens,
        int completionTokens,
        int totalTokens,
        double estimatedCostUsd
) {}
