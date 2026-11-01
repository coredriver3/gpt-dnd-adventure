package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record SceneIntro(
        @JsonPropertyDescription("opening narration, 80 to 150 words, in two or three paragraphs separated by a blank line, without dashes")
        String narration,
        @JsonPropertyDescription("short name of the current location")
        String location,
        @JsonPropertyDescription("the starting items the character carries, as short item names, exactly as many as the starting gear roll grants")
        List<String> startingItems,
        @JsonPropertyDescription("exactly three short, distinct actions the player could take next")
        List<String> suggestedActions,
        @JsonPropertyDescription("English prompt for a cinematic illustration of the scene")
        String imagePrompt,
        @JsonPropertyDescription("what the character concretely wants right now and why, one sentence")
        String goal,
        @JsonPropertyDescription("the active danger or opponent, what it wants and how it is hunting the character, one sentence")
        String threat,
        @JsonPropertyDescription("named people introduced so far, each as: name, who they are, what they want from the character")
        List<String> characters,
        @JsonPropertyDescription("unanswered story questions the player could pursue, at most four")
        List<String> openThreads) {
}
