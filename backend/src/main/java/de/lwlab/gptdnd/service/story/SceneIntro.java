package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record SceneIntro(
        @JsonPropertyDescription("opening narration, 80 to 150 words")
        String narration,
        @JsonPropertyDescription("short name of the current location")
        String location,
        @JsonPropertyDescription("the starting items the character carries, as short item names, exactly as many as the starting gear roll grants")
        List<String> startingItems,
        @JsonPropertyDescription("exactly three short, distinct actions the player could take next")
        List<String> suggestedActions,
        @JsonPropertyDescription("English prompt for a cinematic illustration of the scene")
        String imagePrompt) {
}
