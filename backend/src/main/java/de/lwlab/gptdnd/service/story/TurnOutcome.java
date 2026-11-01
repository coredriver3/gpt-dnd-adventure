package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record TurnOutcome(
        @JsonPropertyDescription("narration of what happens, 60 to 140 words, in two or three paragraphs separated by a blank line, without dashes, containing story text only and never these field names")
        String narration,
        @JsonPropertyDescription("short name of the location after this turn")
        String location,
        int healthDelta,
        int resolveDelta,
        @JsonPropertyDescription("items newly obtained in this turn's narration, at most two")
        List<String> itemsGained,
        @JsonPropertyDescription("items from the current inventory that are lost, used up or broken in this turn's narration")
        List<String> itemsLost,
        @JsonPropertyDescription("coins newly found or spent in this turn's narration; 0 if none, never count coins again that were found earlier")
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
        String imagePrompt,
        @JsonPropertyDescription("what the character concretely wants now and why, one sentence, updated when it changes")
        String goal,
        @JsonPropertyDescription("the active danger or opponent, what it wants and what it is doing now, one sentence")
        String threat,
        @JsonPropertyDescription("all named people so far, each as: name, who they are, how they stand toward the character")
        List<String> characters,
        @JsonPropertyDescription("unanswered story questions after this turn, at most four; drop what has been resolved")
        List<String> openThreads) {
}
