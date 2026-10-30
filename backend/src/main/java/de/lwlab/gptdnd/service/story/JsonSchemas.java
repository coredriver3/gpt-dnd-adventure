package de.lwlab.gptdnd.service.story;

import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import java.util.Map;

/**
 * Derives strict JSON schemas (all fields required, no additional properties) from the response records.
 * Uses the Anthropic SDK's schema generator so every provider gets the same schema.
 */
final class JsonSchemas {
    private JsonSchemas() {
    }

    static OutputConfig outputConfig(Class<?> type) {
        return MessageCreateParams.builder()
                .model("schema")
                .maxTokens(1L)
                .addUserMessage("schema")
                .outputConfig(type)
                .build()
                .rawParams()
                .outputConfig()
                .orElseThrow();
    }

    static String json(Class<?> type) {
        Map<String, JsonValue> schema = outputConfig(type).format().orElseThrow().schema()._additionalProperties();
        try {
            return ObjectMappers.jsonMapper().writeValueAsString(schema);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize schema for " + type.getSimpleName(), exception);
        }
    }
}
