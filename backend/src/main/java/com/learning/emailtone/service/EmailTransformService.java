package com.learning.emailtone.service;

import com.learning.emailtone.dto.TokenUsageDto;
import com.learning.emailtone.dto.TransformRequest;
import com.learning.emailtone.dto.TransformResponse;
import com.learning.emailtone.exception.LlmException;
import com.learning.emailtone.model.LlmTransformOutput;
import com.learning.emailtone.model.Tone;
import com.learning.emailtone.util.TokenCostCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core orchestration: render the prompt, call the LLM, parse structured
 * output, attach token usage, return a typed response.
 *
 * <h2>Why a single LLM call</h2>
 * Tone-shifting is intrinsically a single-turn task: input email + tone in,
 * rewritten email out. There's no batching benefit, no map/reduce, no parallelism.
 * Keep it boring.
 *
 * <h2>What @Retryable buys us here</h2>
 * Two real failure modes:
 *   1. Transient HTTP errors from OpenAI (5xx, 429 after our local quota burst).
 *   2. Malformed JSON output. Even at temperature 0.7 with a strict schema, the
 *      model occasionally emits ```json fences, trailing commas, or truncates
 *      mid-string. {@link BeanOutputConverter#convert} throws on any of these.
 *
 * Both surface as {@link LlmException} (we wrap them at the call site), so a
 * single {@code @Retryable} clause covers both. Max 3 attempts with 1s→2s
 * backoff; beyond that, the upstream is genuinely down and we should fail fast.
 *
 * <p>{@code @Retryable} works through Spring AOP proxies — calling this method
 * via the injected bean activates the proxy. Self-invocation inside the same
 * class would NOT trigger retry. {@code @EnableRetry} on the application class
 * is what wires the proxy in.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTransformService {

    private final ChatClient chatClient;
    private final TokenCostCalculator costCalculator;

    // Loaded once at startup; .text() will template-substitute placeholders per call.
    @Value("classpath:prompts/transform-prompt.st")
    private Resource transformPromptResource;

    @Retryable(
            retryFor = LlmException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public TransformResponse transform(TransformRequest request) {
        log.info("Transform request: tone={} originalLen={} preserveLength={} hasContext={}",
                request.tone(),
                request.originalEmail() == null ? 0 : request.originalEmail().length(),
                Boolean.TRUE.equals(request.preserveLength()),
                request.context() != null && !request.context().isBlank());

        // ---------- 1) Build the structured-output converter ----------
        // KEY LEARNING: BeanOutputConverter is the workhorse of structured AI
        // output in Spring AI. It does TWO things in one object:
        //   (a) .getFormat() returns a JSON-schema-and-instruction string that
        //       we splice into the prompt at {format}. The model then knows
        //       the exact shape to produce.
        //   (b) .convert(rawText) parses the model's reply into a typed
        //       LlmTransformOutput via Jackson, throwing on bad input.
        BeanOutputConverter<LlmTransformOutput> converter =
                new BeanOutputConverter<>(LlmTransformOutput.class);
        String formatInstruction = converter.getFormat();

        // ---------- 2) Render prompt sections that are conditionally included ----------
        // We pre-render the variable-length sections so the .st template can stay
        // simple. Putting if/else inside the template is possible but harder to read.
        Tone tone = request.tone();
        String contextSection = renderContextSection(request.context());
        String lengthInstruction = renderLengthInstruction(Boolean.TRUE.equals(request.preserveLength()));

        Map<String, Object> params = new HashMap<>();
        params.put("tone", tone.name());
        params.put("toneDescription", tone.description());
        params.put("toneInstruction", tone.aiInstruction());
        params.put("contextSection", contextSection);
        params.put("lengthInstruction", lengthInstruction);
        params.put("originalEmail", request.originalEmail());
        params.put("format", formatInstruction);

        // ---------- 3) Invoke the ChatClient ----------
        // We use .chatResponse() (not .entity()) because we want BOTH the
        // typed output AND the usage metadata. .entity() would hand us only
        // the parsed object and discard the response wrapper.
        ChatResponse chatResponse;
        try {
            chatResponse = chatClient.prompt()
                    .user(spec -> spec
                            .text(readResource(transformPromptResource))
                            .params(params))
                    .call()
                    .chatResponse();
        } catch (RuntimeException e) {
            // Wrap any Spring AI / network error so @Retryable sees a single type.
            throw new LlmException("LLM call failed: " + e.getMessage(), e);
        }
        if (chatResponse == null || chatResponse.getResult() == null) {
            throw new LlmException("LLM returned an empty response.");
        }

        // Spring AI 1.0 renamed AssistantMessage.getContent() -> .getText().
        String rawText = chatResponse.getResult().getOutput().getText();
        log.debug("Raw LLM output ({} chars)", rawText == null ? 0 : rawText.length());

        // ---------- 4) Parse the structured output ----------
        LlmTransformOutput parsed;
        try {
            parsed = converter.convert(rawText);
        } catch (RuntimeException e) {
            // Malformed JSON triggers a retry via @Retryable.
            throw new LlmException("LLM output could not be parsed into expected schema.", e);
        }
        if (parsed == null || parsed.transformedEmail() == null || parsed.transformedEmail().isBlank()) {
            throw new LlmException("LLM produced no transformed email.");
        }

        // ---------- 5) Token usage + cost ----------
        Usage usage = chatResponse.getMetadata().getUsage();
        TokenUsageDto tokenUsage = costCalculator.toUsage(
                safeInt(usage.getPromptTokens()),
                safeInt(usage.getCompletionTokens()));

        // ---------- 6) Assemble the wire DTO ----------
        return new TransformResponse(
                UUID.randomUUID(),
                request.originalEmail(),
                parsed.transformedEmail(),
                tone,
                parsed.changesNotes(),
                tokenUsage);
    }

    private String renderContextSection(String context) {
        if (context == null || context.isBlank()) {
            return ""; // omit entirely — see the .st template, this slots between sections
        }
        // The leading newlines are deliberate: they create a clean visual break
        // in the assembled prompt without us needing template logic.
        return "\nContext about the recipient/situation:\n" + context.trim() + "\n";
    }

    private String renderLengthInstruction(boolean preserve) {
        return preserve
                ? "Keep the rewritten email approximately the same length as the original."
                : "You may shorten or lengthen the email as appropriate to the requested tone.";
    }

    private String readResource(Resource r) {
        try {
            return r.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LlmException("Failed to read prompt template.", e);
        }
    }

    /**
     * Null-safe narrowing. Spring AI's {@link Usage} returns wrapper types
     * (Integer/Long depending on version) that can be null when metadata is
     * absent — treat missing as zero rather than NPE.
     */
    private int safeInt(Number v) {
        if (v == null) return 0;
        long l = v.longValue();
        if (l < 0) return 0;
        return (int) Math.min(l, Integer.MAX_VALUE);
    }
}
