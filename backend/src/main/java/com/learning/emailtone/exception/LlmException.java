package com.learning.emailtone.exception;

/**
 * Raised when the LLM call fails OR its response can't be parsed into our
 * expected schema.
 *
 * <p>Both flavors share a single exception type because they share a single
 * remediation: retry. {@code @Retryable(retryFor = LlmException.class)} on
 * the service catches both — transient HTTP failures AND occasional malformed
 * JSON output — without disguising real bugs as flakiness (a NullPointerException
 * in our code wouldn't be wrapped, so it won't be retried).
 */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
