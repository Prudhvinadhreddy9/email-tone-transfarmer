package com.learning.emailtone.model;

import java.util.List;

/**
 * The JSON shape the LLM is asked to produce.
 *
 * <p>Spring AI's {@code BeanOutputConverter<LlmTransformOutput>} does two jobs:
 * <ol>
 *   <li><b>At prompt time:</b> generates a JSON schema from this record's
 *       components and embeds it in the prompt where {@code {format}} appears.
 *       The schema names ARE the JSON keys — choose them carefully.</li>
 *   <li><b>At parse time:</b> Jackson-deserializes the model's reply into this
 *       record. Throws if the JSON is malformed or fields are missing.</li>
 * </ol>
 *
 * <p>WHY this is a separate record from {@code TransformResponse} (the wire DTO):
 * the LLM produces only the *AI-derived* fields. The server adds the
 * transformId, echoes the original email, attaches token usage, etc. Keeping
 * "what the model emits" separate from "what we return" means we can change
 * either independently without prompt rework.
 *
 * @param transformedEmail the rewritten email text — the main payload
 * @param changesNotes     short bullet list of edits the model made; lets the
 *                         user understand the diff without computing one
 */
public record LlmTransformOutput(
        String transformedEmail,
        List<String> changesNotes
) {}
