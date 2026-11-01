package de.lwlab.gptdnd.service.rules;

import de.lwlab.gptdnd.domain.Beat;

/** The director's instructions for one turn. {@code threatClock} is the value to store after this turn. */
public record Direction(
        Beat beat,
        int threatClock,
        boolean threatStrikes,
        boolean introduceFigure,
        boolean changeScenery,
        boolean coinsAllowed) {
}
