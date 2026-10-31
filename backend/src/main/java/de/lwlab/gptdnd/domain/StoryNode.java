package de.lwlab.gptdnd.domain;

import java.time.Instant;
import java.util.List;

public record StoryNode(
        String id,
        String type,
        String action,
        String text,
        String rollText,
        DiceRoll roll,
        String imagePrompt,
        List<Consequence> consequences,
        Instant createdAt) {
    public static final String SCENE = "scene";
    public static final String STORY = "story point";
    public static final String COMMAND = "command";
    public static final String REJECTED = "rejected input";
    public static final String SYSTEM = "system";
}
