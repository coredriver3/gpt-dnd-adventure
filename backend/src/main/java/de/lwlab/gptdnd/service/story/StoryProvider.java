package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;

public interface StoryProvider {
    SceneIntro intro(GameSession session, StartingGear gear);

    ActionPlan plan(GameSession session, String action);

    /** {@code roll} is null when the plan needed no roll. */
    TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll);
}
