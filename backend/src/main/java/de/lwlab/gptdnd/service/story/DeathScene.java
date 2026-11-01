package de.lwlab.gptdnd.service.story;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record DeathScene(
        @JsonPropertyDescription("the rewritten narration of this turn, ending with the character's death, 60 to 140 words, in two or three paragraphs separated by a blank line, without dashes")
        String narration) {
}
