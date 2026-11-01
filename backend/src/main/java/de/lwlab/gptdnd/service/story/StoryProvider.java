package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.Direction;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;

public interface StoryProvider {
    SceneIntro intro(GameSession session, StartingGear gear);

    ActionPlan plan(GameSession session, String action);

    /** {@code roll} is null when the plan decided the outcome without dice. */
    TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll, Direction direction);

    /** Rewrites a turn that turned out to be fatal; the narration was written before the backend applied the damage. */
    DeathScene death(GameSession session, String action, String narration);
}
