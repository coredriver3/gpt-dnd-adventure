package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.RollOutcome;

public record StoryPrompt(
        GameSession session,
        String action,
        String place,
        Integer naturalRoll,
        RollOutcome outcome,
        boolean command) {
}
