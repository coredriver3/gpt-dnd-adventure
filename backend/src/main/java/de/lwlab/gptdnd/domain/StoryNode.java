package de.lwlab.gptdnd.domain;

import java.time.Instant;
import java.util.List;

public record StoryNode(
        String id,
        String type,
        String text,
        String rollText,
        String imagePrompt,
        List<Consequence> consequences,
        Instant createdAt) {
}
