package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record TurnOutcome(
        @JsonPropertyDescription("narration of what happens, 60 to 140 words")
        String narration,
        @JsonPropertyDescription("short name of the location after this turn")
        String location,
        int healthDelta,
        int resolveDelta,
        @JsonPropertyDescription("items the character plausibly obtains this turn, at most two")
        List<String> itemsGained,
        @JsonPropertyDescription("items from the current inventory that are lost, used up or broken")
        List<String> itemsLost,
        int coinDelta,
        int rationsDelta,
        int torchDelta,
        @JsonPropertyDescription("new lasting conditions, e.g. bleeding, hunted by the watch")
        List<String> effectsAdded,
        @JsonPropertyDescription("existing effects that end this turn")
        List<String> effectsRemoved,
        boolean characterDied,
        @JsonPropertyDescription("exactly three short, distinct actions the player could take next")
        List<String> suggestedActions,
        @JsonPropertyDescription("English prompt for a cinematic illustration of the moment")
        String imagePrompt) {
}
