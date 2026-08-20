package de.lwlab.gptdnd.api;

import java.time.Instant;
import java.util.List;

public record StoryNodeResponse(
        String id,
        String type,
        String text,
        String rollText,
        String imagePrompt,
        List<ConsequenceResponse> consequences,
        Instant createdAt) {
}
