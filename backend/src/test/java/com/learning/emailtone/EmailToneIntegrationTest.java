package com.learning.emailtone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emailtone.dto.TransformRequest;
import com.learning.emailtone.model.Tone;
import com.learning.emailtone.testutil.StubChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test — boots the entire Spring context but swaps the OpenAI
 * ChatModel for a {@link StubChatModel} via {@link TestConfiguration}.
 *
 * <p>WHY {@code @Primary} on the stub bean: the OpenAI starter creates an
 * auto-configured ChatModel bean. Marking ours @Primary takes precedence
 * without having to exclude the auto-config (which would cascade into
 * disabling several other beans we DO want).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailToneIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void healthEndpoint_reportsConfigurationState() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // test profile uses the "test-" placeholder key, so configured = false
                .andExpect(jsonPath("$.openAiConfigured").value(false))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"));
    }

    @Test
    void tonesEndpoint_listsAllSixTones() throws Exception {
        mvc.perform(get("/api/tones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.id == 'PROFESSIONAL')].displayName").value("Professional"))
                // aiInstruction must NOT leak — see ToneInfo javadoc
                .andExpect(jsonPath("$[0].aiInstruction").doesNotExist());
    }

    @Test
    void transformEndpoint_endToEnd_withStubLlm() throws Exception {
        TransformRequest req = new TransformRequest(
                "Hey can u send the report ASAP I need it now",
                Tone.PROFESSIONAL,
                "my manager",
                false);

        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.transformedEmail").value(
                        org.hamcrest.Matchers.containsString("Dear")))
                .andExpect(jsonPath("$.changesNotes.length()").value(3))
                .andExpect(jsonPath("$.tokenUsage.promptTokens").value(412));
    }

    @TestConfiguration
    static class StubAiConfig {
        @Bean
        @Primary
        ChatModel stubChatModel() {
            String canned = """
                {
                  "transformedEmail": "Dear team,\\n\\nCould you please share the report at your earliest convenience? It is needed for an urgent review.\\n\\nKind regards,\\n[Sender]",
                  "changesNotes": [
                    "Added a formal greeting and closing",
                    "Replaced 'ASAP' with a polite request",
                    "Removed casual phrasing ('u' -> 'you')"
                  ]
                }
                """;
            return new StubChatModel(p -> canned, 412, 89);
        }
    }
}
