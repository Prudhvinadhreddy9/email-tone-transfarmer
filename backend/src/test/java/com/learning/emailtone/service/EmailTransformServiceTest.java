package com.learning.emailtone.service;

import com.learning.emailtone.dto.TransformRequest;
import com.learning.emailtone.dto.TransformResponse;
import com.learning.emailtone.exception.LlmException;
import com.learning.emailtone.model.Tone;
import com.learning.emailtone.testutil.StubChatModel;
import com.learning.emailtone.util.TokenCostCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-layer unit tests.
 *
 * <p>We use a real {@link ChatClient} bolted onto a {@link StubChatModel}
 * (see StubChatModel javadoc for why), and inject the real prompt template
 * via {@link ReflectionTestUtils} since Spring isn't bootstrapped here.
 */
class EmailTransformServiceTest {

    private final TokenCostCalculator costCalculator = new TokenCostCalculator();

    private EmailTransformService buildService(StubChatModel stub) {
        ChatClient chatClient = ChatClient.builder(stub)
                .defaultSystem("test-system-prompt")
                .build();
        EmailTransformService service = new EmailTransformService(chatClient, costCalculator);
        // The @Value-injected resource is null without Spring; set it directly.
        ReflectionTestUtils.setField(service, "transformPromptResource",
                new ClassPathResource("prompts/transform-prompt.st"));
        return service;
    }

    @Test
    void rendersPromptCorrectlyAndParsesResponse() {
        String canned = """
            {
              "transformedEmail": "Dear team,\\n\\nCould you please send me the report at your earliest convenience?\\n\\nKind regards,\\n[Sender]",
              "changesNotes": [
                "Added a formal greeting",
                "Replaced 'ASAP' with a polite request",
                "Added a sign-off"
              ]
            }
            """;
        AtomicReference<String> sentPrompt = new AtomicReference<>();
        StubChatModel stub = new StubChatModel(p -> {
            sentPrompt.set(p.getContents());
            return canned;
        }, 412, 89);

        EmailTransformService service = buildService(stub);

        TransformRequest req = new TransformRequest(
                "Hey can you send me the report ASAP I really need it",
                Tone.PROFESSIONAL,
                "my manager",
                false);
        TransformResponse resp = service.transform(req);

        // ----- Assert prompt rendering -----
        String prompt = sentPrompt.get();
        assertThat(prompt).contains("PROFESSIONAL");
        // tone description should have been injected
        assertThat(prompt).contains(Tone.PROFESSIONAL.description());
        // tone-specific AI instruction should be in the prompt
        assertThat(prompt).contains("formal salutations");
        // user's context should be carried in
        assertThat(prompt).contains("my manager");
        // the original email is quoted in
        assertThat(prompt).contains("Hey can you send me the report ASAP I really need it");
        // {format} placeholder consumed and replaced by a JSON schema fragment
        assertThat(prompt).doesNotContain("{format}");
        assertThat(prompt).contains("transformedEmail"); // schema mentions our field

        // ----- Assert response wiring -----
        assertThat(resp.tone()).isEqualTo(Tone.PROFESSIONAL);
        assertThat(resp.originalEmail()).isEqualTo(req.originalEmail());
        assertThat(resp.transformedEmail()).contains("Dear team");
        assertThat(resp.changesNotes()).hasSize(3);
        assertThat(resp.transformId()).isNotNull();

        // ----- Assert token usage + cost -----
        assertThat(resp.tokenUsage().promptTokens()).isEqualTo(412);
        assertThat(resp.tokenUsage().completionTokens()).isEqualTo(89);
        assertThat(resp.tokenUsage().totalTokens()).isEqualTo(501);
        // 412 * 0.15/1M + 89 * 0.60/1M = 0.0000618 + 0.0000534 = 0.0001152
        assertThat(resp.tokenUsage().estimatedCostUsd()).isEqualTo(0.000115);
    }

    @Test
    void omitsContextSectionWhenContextBlank() {
        StubChatModel stub = new StubChatModel(p -> validResponseJson());
        EmailTransformService service = buildService(stub);

        service.transform(new TransformRequest(
                "Some email content that is at least ten characters long.",
                Tone.FRIENDLY,
                "   ",   // blank
                null));

        String prompt = stub.getLastPrompt().getContents();
        assertThat(prompt).doesNotContain("Context about the recipient");
    }

    @Test
    void includesPreserveLengthInstructionWhenRequested() {
        StubChatModel stub = new StubChatModel(p -> validResponseJson());
        EmailTransformService service = buildService(stub);

        service.transform(new TransformRequest(
                "Some email content that is at least ten characters long.",
                Tone.CONCISE,
                null,
                true));

        String prompt = stub.getLastPrompt().getContents();
        assertThat(prompt).contains("approximately the same length");
    }

    @Test
    void wrapsMalformedJsonAsLlmException() {
        StubChatModel stub = new StubChatModel(p -> "this is not JSON at all");
        EmailTransformService service = buildService(stub);

        assertThatThrownBy(() -> service.transform(new TransformRequest(
                "Some email content that is at least ten characters long.",
                Tone.ASSERTIVE,
                null,
                false)))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("could not be parsed");
    }

    private String validResponseJson() {
        return """
            {
              "transformedEmail": "Hi team, please send the report. Thanks!",
              "changesNotes": ["Tightened phrasing", "Added a greeting"]
            }
            """;
    }
}
