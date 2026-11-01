package de.lwlab.gptdnd.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lwlab.gptdnd.api.ActionRequest;
import de.lwlab.gptdnd.api.CreateSessionRequest;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.StoryNode;
import de.lwlab.gptdnd.service.rules.Direction;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;
import de.lwlab.gptdnd.service.story.ActionPlan;
import de.lwlab.gptdnd.service.story.SceneIntro;
import de.lwlab.gptdnd.service.story.StoryProvider;
import de.lwlab.gptdnd.service.story.TurnOutcome;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class GameServiceTest {
    @Autowired
    private GameService gameService;

    @Autowired
    private StubStoryProvider provider;

    @BeforeEach
    void reset() {
        provider.plan = plan(ActionPlan.Decision.AUTOMATIC_SUCCESS);
        provider.outcome = outcome(0, false, List.of(), List.of(), 0);
    }

    @Test
    void createStoresIntroLocationAndSuggestions() {
        GameSession session = create();

        assertThat(session.location()).isEqualTo("Old mill");
        assertThat(session.suggestedActions()).containsExactly("a", "b", "c");
        assertThat(session.nodes()).singleElement().extracting(StoryNode::type).isEqualTo(StoryNode.SCENE);
    }

    @Test
    void startingItemsAreLimitedByTheRoll() {
        GameSession session = create();

        StoryNode intro = session.nodes().getFirst();
        assertThat(intro.roll()).isNotNull();
        assertThat(session.inventory()).hasSize(StartingGear.of(intro.roll().natural()).itemCount());
    }

    @Test
    void acceptsFreeTextSettings() {
        GameSession session = gameService.create(new CreateSessionRequest("Vex", "ex-pilot", "Rusty space station", "en"));

        assertThat(gameService.get(session.id()).character().setting()).isEqualTo("Rusty space station");
    }

    @Test
    void deletesSessionsWithTheirStory() {
        GameSession session = create();

        gameService.delete(session.id());

        assertThatThrownBy(() -> gameService.get(session.id())).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void clampsProposedChangesToWhatTheDegreeAllows() {
        GameSession session = create();
        provider.outcome = outcome(-10, true, List.of("knife", "dragon egg"), List.of("rope", "lamp", "crown"), -50);

        GameSession after = gameService.act(session.id(), new ActionRequest("I walk to the door"));

        assertThat(after.health()).isEqualTo(10);
        assertThat(after.dead()).isFalse();
        assertThat(after.resources()).containsEntry("coin", 0);
        assertThat(after.inventory()).contains("lamp").doesNotContain("crown");
        assertThat(after.turn()).isEqualTo(1);
        StoryNode node = after.nodes().getLast();
        assertThat(node.action()).isEqualTo("I walk to the door");
        assertThat(node.rollText()).isEqualTo("no roll");
    }

    @Test
    void rolledActionsRecordTheCheck() {
        GameSession session = create();
        provider.plan = new ActionPlan(ActionPlan.Decision.ROLL, "", "Stealth", 40, 9, "you get past", "the guard sees you");

        GameSession after = gameService.act(session.id(), new ActionRequest("I sneak past the guard"));

        assertThat(provider.lastRoll).isNotNull();
        assertThat(provider.lastRoll.difficulty()).isEqualTo(25);
        assertThat(provider.lastRoll.modifier()).isEqualTo(3);
        assertThat(after.nodes().getLast().rollText()).startsWith("Stealth: d20 ");
    }

    @Test
    void invalidActionsDoNotAdvanceTheStory() {
        GameSession session = create();
        provider.plan = new ActionPlan(ActionPlan.Decision.INVALID, "That is not possible here.", "", 0, 0, "", "");

        GameSession after = gameService.act(session.id(), new ActionRequest("I summon a dragon"));

        assertThat(after.turn()).isZero();
        assertThat(after.nodes().getLast().type()).isEqualTo(StoryNode.REJECTED);
        assertThat(after.nodes().getLast().text()).isEqualTo("That is not possible here.");
    }

    @Test
    void replacesDashesAndKeepsParagraphsInNarration() {
        GameSession session = create();
        provider.outcome = new TurnOutcome("The door opens \u2014 slowly.\n\nA draft \u2013 cold - follows.", "Old mill", 0, 0,
                List.of(), List.of(), 0, 0, 0, List.of(), List.of(), false, List.of(), "image", "g", "t", List.of(), List.of());

        GameSession after = gameService.act(session.id(), new ActionRequest("I open the door"));

        assertThat(after.nodes().getLast().text()).isEqualTo("The door opens, slowly.\n\nA draft, cold, follows.");
    }

    @Test
    void automaticOutcomesRollNoDice() {
        GameSession session = create();

        GameSession success = gameService.act(session.id(), new ActionRequest("I open the unlocked door"));
        assertThat(provider.lastRoll).isNull();
        assertThat(success.nodes().getLast().roll()).isNull();
        assertThat(success.nodes().getLast().consequences()).extracting("text").contains("automatic success");

        provider.plan = plan(ActionPlan.Decision.AUTOMATIC_FAILURE);
        GameSession failure = gameService.act(session.id(), new ActionRequest("I jump to the moon"));
        assertThat(provider.lastRoll).isNull();
        assertThat(failure.nodes().getLast().consequences()).extracting("text").contains("automatic failure");
        assertThat(failure.turn()).isEqualTo(2);
    }

    @Test
    void storyStateAndClockArePersistedAcrossTurns() {
        GameSession session = create();
        assertThat(session.goal()).isEqualTo("Reach the mill");

        provider.outcome = new TurnOutcome("Ada steps out.", "Old mill", 0, 0, List.of(), List.of(), 0, 0, 0, List.of(), List.of(), false,
                List.of(), "image", "Bargain with Ada", "", List.of("Ada, a scavenger"), List.of("Who sent her?"));
        gameService.act(session.id(), new ActionRequest("I wait at the door"));

        GameSession stored = gameService.get(session.id());
        assertThat(stored.goal()).isEqualTo("Bargain with Ada");
        assertThat(stored.threat()).isEqualTo("The miller hunts you");
        assertThat(stored.characters()).containsExactly("Ada, a scavenger");
        assertThat(stored.openThreads()).containsExactly("Who sent her?");
        assertThat(stored.threatClock()).isEqualTo(1);
        assertThat(stored.recentBeats()).hasSize(1);
    }

    @Test
    void threatStrikeAllowsExtraHarmEvenWithoutARoll() {
        GameSession session = create();
        provider.outcome = outcome(-9, false, List.of(), List.of(), 0);

        gameService.act(session.id(), new ActionRequest("I wait a moment"));
        gameService.act(session.id(), new ActionRequest("I wait a moment"));
        GameSession after = gameService.act(session.id(), new ActionRequest("I wait a moment"));

        assertThat(after.health()).isEqualTo(8);
    }

    @Test
    void newCoinsAreCappedAndOnCooldown() {
        GameSession session = create();
        provider.outcome = outcome(0, false, List.of(), List.of(), 9);

        GameSession first = gameService.act(session.id(), new ActionRequest("I search the shelf"));
        assertThat(first.resources()).containsEntry("coin", 18);

        GameSession second = gameService.act(session.id(), new ActionRequest("I search the floor"));
        assertThat(second.resources()).containsEntry("coin", 18);
    }

    @Test
    void rewritesTurnsWithTechnicalTextAndStripsItIfItPersists() {
        GameSession session = create();
        provider.outcome = new TurnOutcome("You find a door.\n\nhealthDelta 0 // coinDelta 4", "Old mill", 0, 0, List.of(), List.of(), 0, 0, 0,
                List.of(), List.of(), false, List.of(), "image", "g", "t", List.of(), List.of());
        provider.resolveCalls = 0;

        GameSession after = gameService.act(session.id(), new ActionRequest("I look around"));

        assertThat(provider.resolveCalls).isEqualTo(2);
        assertThat(after.nodes().getLast().text()).isEqualTo("You find a door.");
    }

    @Test
    void keepsPeopleWhenTheModelDeliversGarbageAndCleansNarrationMarkup() {
        GameSession session = create();
        provider.outcome = new TurnOutcome("Ada steps out.", "Old mill", 0, 0, List.of(), List.of(), 0, 0, 0, List.of(), List.of(), false,
                List.of(), "image", "", "", List.of("Ada, a scavenger", ".", "the", "/stable-diffusion/xl/1.0"), List.of());
        GameSession first = gameService.act(session.id(), new ActionRequest("I wait a moment"));
        assertThat(first.characters()).containsExactly("Ada, a scavenger");

        provider.outcome = new TurnOutcome(">You wait.", "Old mill", 0, 0, List.of(), List.of(), 0, 0, 0, List.of(), List.of(), false,
                List.of(), "image", "", "", List.of(".", ">You wait.\n\nMore narration"), List.of());
        GameSession second = gameService.act(session.id(), new ActionRequest("I wait a moment"));

        assertThat(second.characters()).containsExactly("Ada, a scavenger");
        assertThat(second.nodes().getLast().text()).isEqualTo("You wait.");
    }

    @Test
    void canSpendMoreThanTenCoinsButNotMoreThanTheyHave() {
        GameSession session = create();
        provider.outcome = outcome(0, false, List.of(), List.of(), -50);

        GameSession after = gameService.act(session.id(), new ActionRequest("I hand over my purse"));

        assertThat(after.resources()).containsEntry("coin", 0);
    }

    @Test
    void rollCommandNoLongerExists() {
        GameSession session = create();

        GameSession after = gameService.act(session.id(), new ActionRequest("[roll d20+100]"));

        assertThat(after.nodes().getLast().roll()).isNull();
        assertThat(after.nodes().getLast().text()).startsWith("Unknown command");
    }

    @Test
    void commandsDoNotCallTheNarrator() {
        GameSession session = create();
        provider.plan = null;

        GameSession after = gameService.act(session.id(), new ActionRequest("[status]"));

        assertThat(after.nodes().getLast().type()).isEqualTo(StoryNode.COMMAND);
        assertThat(after.nodes().getLast().text()).contains("Location: Old mill");
    }

    @Test
    void listsMostRecentlyPlayedSessionsFirst() {
        GameSession older = create();
        GameSession newer = create();
        gameService.act(older.id(), new ActionRequest("I walk to the door"));

        List<String> ids = gameService.list().stream().map(GameSession::id).toList();

        assertThat(ids.indexOf(older.id())).isLessThan(ids.indexOf(newer.id()));
    }

    private static ActionPlan plan(ActionPlan.Decision decision) {
        return new ActionPlan(decision, "", "", 0, 0, "", "");
    }

    private GameSession create() {
        return gameService.create(new CreateSessionRequest("Mara", "smuggler", "dark-fantasy", "en"));
    }

    private static TurnOutcome outcome(int health, boolean died, List<String> lost, List<String> gained, int coin) {
        return new TurnOutcome("Something happens.", "Old mill", health, 0, gained, lost, coin, 0, 0,
                List.of(), List.of(), died, List.of("x", "y", "z"), "image", "", "", List.of(), List.of());
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        StubStoryProvider stubStoryProvider() {
            return new StubStoryProvider();
        }
    }

    static class StubStoryProvider implements StoryProvider {
        ActionPlan plan;
        TurnOutcome outcome;
        RollResult lastRoll;
        int resolveCalls;

        @Override
        public SceneIntro intro(GameSession session, StartingGear gear) {
            return new SceneIntro("You arrive.", "Old mill", List.of("knife", "map", "ink", "rope", "extra"), List.of("a", "b", "c"), "image",
                    "Reach the mill", "The miller hunts you", List.of(), List.of());
        }

        @Override
        public ActionPlan plan(GameSession session, String action) {
            return plan;
        }

        @Override
        public TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll, Direction direction) {
            lastRoll = roll;
            resolveCalls++;
            return outcome;
        }
    }
}
