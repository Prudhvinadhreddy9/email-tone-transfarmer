package com.learning.emailtone.dto;

/**
 * What {@code GET /api/tones} returns per tone.
 *
 * <p>WHY this is a slimmed-down DTO instead of just serializing the {@code Tone}
 * enum directly: the enum has an {@code aiInstruction} field that is prompt
 * internals — we don't want to expose it on the public API. Browsers don't
 * need it, and shipping prompt internals over the wire makes prompt-injection
 * attacks easier and prompt iteration harder (every change becomes a public
 * contract change).
 *
 * @param id          enum constant name — what the frontend echoes back in
 *                    {@code TransformRequest.tone}
 * @param displayName human-readable label for the UI button
 * @param description tooltip text describing when to use this tone
 */
public record ToneInfo(
        String id,
        String displayName,
        String description
) {}
