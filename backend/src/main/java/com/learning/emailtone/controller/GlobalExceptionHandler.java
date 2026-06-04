package com.learning.emailtone.controller;

import com.learning.emailtone.exception.LlmException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * RFC 7807 Problem Details translation layer.
 *
 * <p>WHY Problem Details: standardized machine-readable error shape, native to
 * Spring 6 via {@link ProblemDetail}. Any HTTP client library can parse these
 * without custom error mapping; the React frontend reads the {@code title} /
 * {@code detail} fields directly.
 *
 * <p>Two invariants enforced here:
 * <ol>
 *   <li>Client-facing detail strings never contain stack traces, internal
 *       class names, or framework jargon.</li>
 *   <li>The full exception INCLUDING cause is logged server-side at the right
 *       level — WARN for client errors, ERROR for server-side ones — so ops
 *       can debug.</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://emailtone/problems/validation");
    private static final URI LLM_TYPE = URI.create("https://emailtone/problems/llm");

    /** @Valid violations on the request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Validation error: {}", msg);
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", msg, VALIDATION_TYPE);
    }

    /**
     * Body fails to parse — usually an invalid Tone string or malformed JSON.
     * We tease out the InvalidFormatException case for a friendlier message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onUnreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String detail = "Invalid value for " + ife.getPath().get(ife.getPath().size() - 1).getFieldName()
                    + ": '" + ife.getValue() + "' is not one of the supported tones.";
            log.warn(detail);
            return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, VALIDATION_TYPE);
        }
        log.warn("Malformed request body", ex);
        return problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body could not be parsed.", VALIDATION_TYPE);
    }

    /** LLM upstream failure. 502 because the issue is with our dependency, not the client. */
    @ExceptionHandler(LlmException.class)
    public ProblemDetail onLlm(LlmException ex) {
        log.error("LLM failure (after retries)", ex);
        return problem(HttpStatus.BAD_GATEWAY,
                "AI service unavailable",
                "The transform service is temporarily unable to process this request. Please try again.",
                LLM_TYPE);
    }

    /** Catch-all. Never leak internals — log them, return a boring 500. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again or contact support.",
                null);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, URI type) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        if (type != null) pd.setType(type);
        return pd;
    }
}
