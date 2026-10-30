package de.lwlab.gptdnd.service.story;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.junit.jupiter.api.Test;

class ClaudeStoryProviderTest {
    @Test
    void structuredOutputSchemasPassLocalValidation() {
        var client = AnthropicOkHttpClient.builder().apiKey("test").build();

        assertThatCode(() -> new ClaudeStoryProvider(client, "claude-opus-5-5", new GameMasterPrompts(40))).doesNotThrowAnyException();
    }
}
