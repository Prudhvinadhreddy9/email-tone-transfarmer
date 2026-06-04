package com.learning.emailtone.dto;

import com.learning.emailtone.model.Tone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inbound request body for {@code POST /api/transform}.
 *
 * <p>Validation is enforced by {@code @Valid} on the controller method.
 * Failures bubble up as {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * which the GlobalExceptionHandler maps to a 400 ProblemDetail.
 *
 * <p>WHY a record instead of a class with Lombok: records get a JSON-friendly
 * constructor and accessors out of the box, are immutable by default, and the
 * validation annotations work on record components in Spring 6+.
 *
 * @param originalEmail   the draft to rewrite
 * @param tone            target tone — Jackson maps the JSON string to the enum
 * @param context         optional free-text hint about who you're writing to
 *                        (recipient role, relationship, situation). May be null.
 * @param preserveLength  if true, the prompt asks the model to keep the
 *                        rewritten email approximately the same length as the
 *                        original. False = the model can shorten or lengthen.
 */
public record TransformRequest(
        @NotBlank(message = "originalEmail must not be blank")
        @Size(min = 10, max = 5000, message = "originalEmail must be 10-5000 characters")
        String originalEmail,

        @NotNull(message = "tone is required")
        Tone tone,

        @Size(max = 500, message = "context must be at most 500 characters")
        String context,

        // Boxed Boolean (not boolean) so Jackson can distinguish "absent" from
        // "false". For this field that's mostly cosmetic — we treat null as
        // false in the service — but it's a useful habit for booleans on DTOs.
        Boolean preserveLength
) {}
