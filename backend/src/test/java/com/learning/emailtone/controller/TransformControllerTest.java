package com.learning.emailtone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emailtone.dto.TokenUsageDto;
import com.learning.emailtone.dto.TransformRequest;
import com.learning.emailtone.dto.TransformResponse;
import com.learning.emailtone.exception.LlmException;
import com.learning.emailtone.model.Tone;
import com.learning.emailtone.service.EmailTransformService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test: only the controller + the GlobalExceptionHandler are loaded;
 * the service is mocked. We isolate HTTP concerns (validation, serialization,
 * error mapping) from business logic.
 */
@WebMvcTest(controllers = {TransformController.class, GlobalExceptionHandler.class})
class TransformControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean EmailTransformService service;

    @Test
    void happyPath_returnsTransformedEmail() throws Exception {
        TransformResponse canned = new TransformResponse(
                UUID.randomUUID(),
                "original",
                "transformed",
                Tone.PROFESSIONAL,
                List.of("Added greeting", "Toned down urgency"),
                new TokenUsageDto(100, 50, 150, 0.000045));
        when(service.transform(any())).thenReturn(canned);

        TransformRequest req = new TransformRequest(
                "Hey, send me the report ASAP please!",
                Tone.PROFESSIONAL,
                null,
                false);

        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformedEmail").value("transformed"))
                .andExpect(jsonPath("$.tone").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.changesNotes.length()").value(2))
                .andExpect(jsonPath("$.tokenUsage.totalTokens").value(150));
    }

    @Test
    void rejectsTooShortEmail() throws Exception {
        String body = """
            {"originalEmail":"hi","tone":"PROFESSIONAL"}
            """;
        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void rejectsMissingTone() throws Exception {
        String body = """
            {"originalEmail":"This email is definitely longer than ten characters."}
            """;
        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void rejectsInvalidToneEnum() throws Exception {
        String body = """
            {"originalEmail":"This email is plenty long enough.","tone":"DRAMATIC"}
            """;
        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("DRAMATIC")));
    }

    @Test
    void mapsLlmExceptionToBadGateway() throws Exception {
        when(service.transform(any())).thenThrow(new LlmException("upstream broke"));

        TransformRequest req = new TransformRequest(
                "This is a long enough email body.",
                Tone.FRIENDLY,
                null,
                false);

        mvc.perform(post("/api/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("AI service unavailable"));
    }
}
