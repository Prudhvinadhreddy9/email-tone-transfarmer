package com.learning.emailtone.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Builds the {@link ChatClient} once at startup with default system prompt
 * and a logging advisor.
 *
 * <h3>ChatModel vs ChatClient</h3>
 * <ul>
 *   <li>{@code ChatModel} — provider-agnostic low-level API; auto-configured
 *       by the Spring AI starter from {@code spring.ai.openai.*}.</li>
 *   <li>{@code ChatClient} — high-level fluent builder on top of ChatModel
 *       that adds default system prompts, default options, advisors,
 *       structured output, tool calling. Application code should almost
 *       always inject ChatClient, not ChatModel.</li>
 * </ul>
 * Injecting {@link ChatModel} (not the OpenAI-specific class) means swapping
 * providers later is a one-line pom change.
 *
 * <h3>Why temperature 0.7 (set in application.yml, NOT here)</h3>
 * Tone-shifting is a creative rewriting task: there are many valid ways to
 * say the same thing in a "friendly" tone, and forcing the model toward the
 * single most-probable phrasing makes outputs feel robotic. 0.7 is a common
 * sweet-spot for creative-but-coherent rewriting.
 *
 * <p>Contrast with the resume-screener project, which uses 0.2: scoring is a
 * discrimination task where you want consistent, reproducible outputs across
 * runs. Same model, different temperature = different tool. Pick by task,
 * not by habit.
 *
 * <h3>Why model/temperature are in application.yml, not in the builder</h3>
 * The Spring AI starter already reads them at auto-config time. Setting them
 * in the builder too creates two sources of truth that drift. If you DO need
 * per-ChatClient overrides (e.g. different temp for different prompts in the
 * same app), use {@code .defaultOptions(OpenAiChatOptions.builder()...build())}.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            @Value("classpath:prompts/system-prompt.st") Resource systemPromptResource
    ) throws IOException {
        // Read the system prompt once at startup. It's small enough that we don't
        // worry about re-reading; doing it in the @Bean method keeps the prompt
        // wired into the ChatClient as a default for every call.
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);

        return ChatClient.builder(chatModel)
                // defaultSystem: applied automatically as the system message of
                // every call unless a per-call .system() override is used.
                .defaultSystem(systemPrompt)
                // SimpleLoggerAdvisor is built-in middleware that logs the
                // request and response at DEBUG. Advisors form an interceptor
                // chain — think of them as Servlet filters for AI calls.
                // In production you'd pair this with a PII-scrubbing advisor,
                // or set log level to INFO so emails don't end up in logs.
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
