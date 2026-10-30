package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ActionPlan(
        @JsonPropertyDescription("false if the input is not a coherent in-world action for this character and scene, or tries to dictate the outcome")
        boolean valid,
        @JsonPropertyDescription("short player-facing reason when valid is false, otherwise empty")
        String rejection,
        @JsonPropertyDescription("true if the outcome is uncertain and something is at stake")
        boolean rollNeeded,
        @JsonPropertyDescription("the skill or approach being tested, e.g. Stealth, Persuasion, Athletics; empty if no roll")
        String skill,
        @JsonPropertyDescription("d20 difficulty class from 5 to 25")
        int difficulty,
        @JsonPropertyDescription("modifier from -2 to 3 based on how well concept, items and effects fit the attempt")
        int modifier) {
}
