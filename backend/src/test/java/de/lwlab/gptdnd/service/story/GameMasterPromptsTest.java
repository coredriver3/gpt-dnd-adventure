package de.lwlab.gptdnd.service.story;

import static org.assertj.core.api.Assertions.assertThat;

import de.lwlab.gptdnd.domain.Beat;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.Degree;
import de.lwlab.gptdnd.service.rules.Direction;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GameMasterPromptsTest {
    private final GameMasterPrompts prompts = new GameMasterPrompts(40);
    private final GameSession session = new GameSession("s", new CharacterProfile("Mirka", "ferrywoman", "flooded port"), Instant.now());

    @Test
    void introAsksForGearAndStoryStateInTheGameLanguage() {
        assertThat(prompts.intro(session, StartingGear.of(14)))
                .contains("d20 rolled 14", "exactly 3 starting item(s)", "openThreads and layout in German");
    }

    @Test
    void planOffersAllDecisions() {
        assertThat(prompts.plan(session, "I jump to the moon"))
                .contains("AUTOMATIC_SUCCESS", "AUTOMATIC_FAILURE", "ROLL", "INVALID", "I jump to the moon", "Recent difficulties: none");
    }

    @Test
    void resolveCarriesTheStakesAndTheDirectorsInstructions() {
        ActionPlan plan = new ActionPlan(ActionPlan.Decision.ROLL, "", "Stealth", 14, 1, "you reach the boat", "the dog barks");
        RollResult roll = new RollResult("Stealth", 5, 1, 14, Degree.FAILURE);
        Direction direction = new Direction(Beat.ENCOUNTER, 3, true, true, true, false);

        assertThat(prompts.resolve(session, "I sneak past", plan, roll, direction))
                .contains("At stake on failure: the dog barks", "ENCOUNTER moment", "The threat acts now",
                        "new named person", "surroundings must change", "healthDelta -5..0", "no new coins this turn", "openThreads in German");
    }

    @Test
    void contextCarriesCoinsAndTheLayout() {
        session.layout("A narrow stairwell with a bolted door at the top.");

        assertThat(prompts.plan(session, "I climb"))
                .contains("coins: 12", "layout: A narrow stairwell with a bolted door at the top.")
                .doesNotContain("resolve:", "rations", "torch");
    }

    @Test
    void successWithCostDemandsACost() {
        ActionPlan plan = new ActionPlan(ActionPlan.Decision.ROLL, "", "Stealth", 14, 1, "a", "b");
        RollResult roll = new RollResult("Stealth", 11, 1, 14, Degree.PARTIAL_SUCCESS);

        assertThat(prompts.resolve(session, "I sneak past", plan, roll, new Direction(Beat.DISCOVERY, 1, false, false, false, true)))
                .contains("must carry a real cost");
    }

    @Test
    void deathRewritesTheTurn() {
        assertThat(prompts.death(session, "I charge", "You charge and stand."))
                .contains("You charge and stand.", "has just died", "end with the death", "in German");
    }

    @Test
    void theModelSettlesTheLayoutBeforeItNarrates() {
        String schema = JsonSchemas.json(TurnOutcome.class);

        assertThat(schema.indexOf("\"layout\"")).isPositive().isLessThan(schema.indexOf("\"narration\""));
    }

    @Test
    void resolveDescribesAutomaticOutcomes() {
        ActionPlan plan = new ActionPlan(ActionPlan.Decision.AUTOMATIC_FAILURE, "", "", 0, 0, "", "");
        Direction direction = new Direction(Beat.DISCOVERY, 1, false, false, false, true);

        assertThat(prompts.resolve(session, "I jump to the moon", plan, null, direction))
                .contains("No roll: this cannot work", "Threat pressure is 1 of 6", "up to +6");
    }
}
