package com.learning.emailtone.controller;

import com.learning.emailtone.dto.TransformRequest;
import com.learning.emailtone.dto.TransformResponse;
import com.learning.emailtone.service.EmailTransformService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/transform — accept an email + target tone, return a rewrite.
 *
 * <p>{@code @Valid} on the request body activates Bean Validation against the
 * record's annotations ({@code @NotBlank}, {@code @Size}, {@code @NotNull}).
 * Failures throw {@link org.springframework.web.bind.MethodArgumentNotValidException},
 * which the GlobalExceptionHandler maps to a 400 ProblemDetail.
 *
 * <p>This controller stays deliberately thin — no business logic, no prompt
 * concerns. All it does is HTTP plumbing + delegation. Keeping controllers
 * thin lets us swap the transport (HTTP, gRPC, queue consumer) without
 * touching the AI logic.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransformController {

    private final EmailTransformService service;

    @PostMapping("/transform")
    public ResponseEntity<TransformResponse> transform(@Valid @RequestBody TransformRequest request) {
        return ResponseEntity.ok(service.transform(request));
    }
}
