package de.lwlab.gptdnd.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import de.lwlab.gptdnd.service.story.ClaudeStoryProvider;
import de.lwlab.gptdnd.service.story.GameMasterPrompts;
import de.lwlab.gptdnd.service.story.LocalStoryProvider;
import de.lwlab.gptdnd.service.story.OpenRouterStoryProvider;
import de.lwlab.gptdnd.service.story.StoryProvider;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class StoryProviderConfig {
    private static final Logger log = LoggerFactory.getLogger(StoryProviderConfig.class);

    @Bean
    public StoryProvider storyProvider(
            @Value("${adventure.story.provider:auto}") String provider,
            @Value("${adventure.ai.api-key:}") String claudeKey,
            @Value("${adventure.ai.model:claude-opus-5-5}") String claudeModel,
            @Value("${adventure.openrouter.api-key:}") String openRouterKey,
            @Value("${adventure.openrouter.model:nvidia/nemotron-3.5-super-vl:free}") String openRouterModel,
            @Value("${adventure.ai.history-nodes:40}") int historyNodes) {
        boolean claudeCredentials = !claudeKey.isBlank() || System.getenv("ANTHROPIC_AUTH_TOKEN") != null;
        String selected = switch (provider) {
            case "claude", "openrouter", "local" -> provider;
            case "auto" -> claudeCredentials ? "claude" : !openRouterKey.isBlank() ? "openrouter" : "local";
            default -> throw new IllegalStateException("Unknown adventure.story.provider: " + provider);
        };
        GameMasterPrompts prompts = new GameMasterPrompts(historyNodes);
        return switch (selected) {
            case "claude" -> {
                log.info("Story provider: Claude ({})", claudeModel);
                yield new ClaudeStoryProvider(claudeClient(claudeKey), claudeModel, prompts);
            }
            case "openrouter" -> {
                log.info("Story provider: OpenRouter ({})", openRouterModel);
                yield new OpenRouterStoryProvider(openRouterClient(openRouterKey), openRouterModel, prompts);
            }
            default -> {
                log.info("Story provider: local narrator (set ANTHROPIC_API_KEY or OPENROUTER_API_KEY to use a model)");
                yield new LocalStoryProvider();
            }
        };
    }

    private AnthropicClient claudeClient(String apiKey) {
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .fromEnv()
                .timeout(Duration.ofMinutes(2))
                .maxRetries(2);
        if (!apiKey.isBlank()) {
            builder.apiKey(apiKey);
        }
        return builder.build();
    }

    private RestClient openRouterClient(String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofMinutes(3));
        return RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-Title", "GPT Dice Adventure")
                .build();
    }
}
