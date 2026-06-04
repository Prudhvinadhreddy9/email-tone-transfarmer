package com.learning.emailtone.testutil;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.function.Function;

/**
 * A hand-rolled {@link ChatModel} stub for tests.
 *
 * <p>WHY not Mockito on the ChatClient fluent chain: {@code prompt().user(...).call().chatResponse()}
 * returns a different spec type at each step. Mocking each layer is brittle
 * and reads terribly. The clean approach is to plug a fake {@link ChatModel}
 * underneath a REAL {@link org.springframework.ai.chat.client.ChatClient} —
 * the fluent chain runs for real (so you exercise prompt rendering and
 * structured-output conversion) and only the call-site that asks for a
 * response is faked.
 *
 * <p>Construct with a function that takes the fully-rendered Prompt — so
 * tests can assert what was actually sent to the LLM — and returns the raw
 * assistant text the model "would have" produced.
 */
public class StubChatModel implements ChatModel {

    private final Function<Prompt, String> responder;
    private final int promptTokens;
    private final int completionTokens;
    private Prompt lastPrompt;

    public StubChatModel(Function<Prompt, String> responder) {
        this(responder, 100, 50);
    }

    public StubChatModel(Function<Prompt, String> responder, int promptTokens, int completionTokens) {
        this.responder = responder;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public Prompt getLastPrompt() {
        return lastPrompt;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        this.lastPrompt = prompt;
        String text = responder.apply(prompt);
        AssistantMessage msg = new AssistantMessage(text);
        Generation gen = new Generation(msg);
        Usage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(gen), metadata);
    }
}
