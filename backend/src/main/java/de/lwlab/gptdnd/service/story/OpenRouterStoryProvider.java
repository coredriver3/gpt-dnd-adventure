package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.Direction;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Game master backed by any chat model on OpenRouter (OpenAI-compatible API). Uses the same prompts and
 * response schemas as {@link ClaudeStoryProvider}; the backend still enforces all rules.
 */
public class OpenRouterStoryProvider implements StoryProvider {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RestClient client;
    private final String model;
    private final GameMasterPrompts prompts;
    private final Map<Class<?>, JsonNode> schemas;

    public OpenRouterStoryProvider(RestClient client, String model, GameMasterPrompts prompts) {
        this.client = client;
        this.model = model;
        this.prompts = prompts;
        this.schemas = Map.of(
                SceneIntro.class, MAPPER.readTree(JsonSchemas.json(SceneIntro.class)),
                ActionPlan.class, MAPPER.readTree(JsonSchemas.json(ActionPlan.class)),
                TurnOutcome.class, MAPPER.readTree(JsonSchemas.json(TurnOutcome.class)),
                DeathScene.class, MAPPER.readTree(JsonSchemas.json(DeathScene.class)));
    }

    @Override
    public SceneIntro intro(GameSession session, StartingGear gear) {
        return call(session, SceneIntro.class, prompts.intro(session, gear));
    }

    @Override
    public ActionPlan plan(GameSession session, String action) {
        return call(session, ActionPlan.class, prompts.plan(session, action));
    }

    @Override
    public TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll, Direction direction) {
        return call(session, TurnOutcome.class, prompts.resolve(session, action, plan, roll, direction));
    }

    @Override
    public DeathScene death(GameSession session, String action, String narration) {
        return call(session, DeathScene.class, prompts.death(session, action, narration));
    }

    private <T> T call(GameSession session, Class<T> type, String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 8000,
                "messages", List.of(
                        Map.of("role", "system", "content", prompts.system(session)),
                        Map.of("role", "user", "content", prompt)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of("name", type.getSimpleName(), "strict", true, "schema", schemas.get(type))));
        // Free models occasionally return malformed JSON, so give each request one retry.
        for (int attempt = 1; ; attempt++) {
            try {
                return MAPPER.readValue(extractJson(send(body)), type);
            } catch (JacksonException exception) {
                if (attempt == 2) {
                    throw new StoryProviderException("The narrator returned an unreadable answer. Try again.", exception);
                }
            }
        }
    }

    private String send(Map<String, Object> body) {
        JsonNode response;
        try {
            response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                throw new StoryProviderException("The free model's rate limit is reached. Try again later.", exception);
            }
            throw new StoryProviderException("The narrator is unavailable (HTTP " + exception.getStatusCode().value() + ").", exception);
        } catch (ResourceAccessException exception) {
            throw new StoryProviderException("The narrator could not be reached.", exception);
        }
        if (response == null || response.has("error")) {
            String message = response == null ? "empty response" : response.path("error").path("message").asString("unknown error");
            throw new StoryProviderException("The narrator failed: " + message, null);
        }
        String content = response.path("choices").path(0).path("message").path("content").asString("");
        if (content.isBlank()) {
            throw new StoryProviderException("The narrator returned no story.", null);
        }
        return content;
    }

    /** Strips Markdown code fences or surrounding prose some models add around the JSON object. */
    private static String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start >= 0 && end > start ? content.substring(start, end + 1) : content;
    }
}
