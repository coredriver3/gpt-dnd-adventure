package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ActionPlan(
        @JsonPropertyDescription("how this action is resolved; ROLL only if the outcome is uncertain and failing would change the story")
        Decision decision,
        @JsonPropertyDescription("short player-facing reason when decision is INVALID, otherwise empty")
        String rejection,
        @JsonPropertyDescription("the skill or approach being tested, e.g. Stealth, Persuasion, Repair; empty unless decision is ROLL")
        String skill,
        @JsonPropertyDescription("d20 difficulty class from 5 to 25 for this specific situation; 0 unless decision is ROLL")
        int difficulty,
        @JsonPropertyDescription("modifier from -2 to 3 based on how well background, items and effects fit the attempt; 0 unless decision is ROLL")
        int modifier,
        @JsonPropertyDescription("what concretely changes if the roll succeeds; empty unless decision is ROLL")
        String onSuccess,
        @JsonPropertyDescription("what concretely goes wrong if the roll fails, moving the story forward instead of stopping it; empty unless decision is ROLL")
        String onFailure) {

    public enum Decision {
        AUTOMATIC_SUCCESS,
        AUTOMATIC_FAILURE,
        ROLL,
        INVALID
    }
}
