package de.lwlab.gptdnd.service.story;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.RollResult;

/**
 * Game master backed by the Claude API. The backend stays the rules authority: Claude judges actions
 * (validity, difficulty) and narrates, while dice and state limits are enforced by {@code GameService}.
 */
public class ClaudeStoryProvider implements StoryProvider {
    private final AnthropicClient client;
    private final String model;
    private final GameMasterPrompts prompts;
    private final OutputConfig introConfig;
    private final OutputConfig planConfig;
    private final OutputConfig turnConfig;

    public ClaudeStoryProvider(AnthropicClient client, String model, GameMasterPrompts prompts) {
        this.client = client;
        this.model = model;
        this.prompts = prompts;
        this.introConfig = outputConfig(SceneIntro.class, OutputConfig.Effort.MEDIUM);
        this.planConfig = outputConfig(ActionPlan.class, OutputConfig.Effort.LOW);
        this.turnConfig = outputConfig(TurnOutcome.class, OutputConfig.Effort.MEDIUM);
    }

    @Override
    public SceneIntro intro(GameSession session) {
        return call(session, SceneIntro.class, introConfig, prompts.intro(session));
    }

    @Override
    public ActionPlan plan(GameSession session, String action) {
        return call(session, ActionPlan.class, planConfig, prompts.plan(session, action));
    }

    @Override
    public TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll) {
        return call(session, TurnOutcome.class, turnConfig, prompts.resolve(session, action, roll));
    }

    private <T> T call(GameSession session, Class<T> type, OutputConfig config, String prompt) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(prompts.system(session))
                .outputConfig(type)
                // Replaces the config above with one that carries both the JSON schema and the effort level.
                .outputConfig(config)
                // On a safety-classifier refusal, let the API retry on its recommended fallback model.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessage(prompt)
                .build();
        try {
            StructuredMessage<T> response = client.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                throw new StoryProviderException("The narrator declined this turn. Try a different action.", null);
            }
            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .findFirst()
                    .orElseThrow(() -> new StoryProviderException("The narrator returned no story.", null));
        } catch (RateLimitException exception) {
            throw new StoryProviderException("The narrator is busy right now. Try again in a moment.", exception);
        } catch (AnthropicServiceException exception) {
            throw new StoryProviderException("The narrator is unavailable (HTTP " + exception.statusCode() + ").", exception);
        } catch (AnthropicException exception) {
            throw new StoryProviderException("The narrator could not be reached.", exception);
        }
    }

    private OutputConfig outputConfig(Class<?> type, OutputConfig.Effort effort) {
        return JsonSchemas.outputConfig(type).toBuilder().effort(effort).build();
    }
}
