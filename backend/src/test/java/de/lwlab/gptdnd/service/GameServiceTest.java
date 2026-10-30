package de.lwlab.gptdnd.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.lwlab.gptdnd.api.ActionRequest;
import de.lwlab.gptdnd.api.CreateSessionRequest;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.StoryNode;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.story.ActionPlan;
import de.lwlab.gptdnd.service.story.SceneIntro;
import de.lwlab.gptdnd.service.story.StoryProvider;
import de.lwlab.gptdnd.service.story.TurnOutcome;
import java.util.List;
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
        provider.plan = new ActionPlan(true, "", false, "", 0, 0);
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
    void clampsProposedChangesToWhatTheDegreeAllows() {
        GameSession session = create();
        provider.outcome = outcome(-10, true, List.of("knife", "dragon egg"), List.of("rope", "lamp", "crown"), -50);

        GameSession after = gameService.act(session.id(), new ActionRequest("I walk to the door"));

        assertThat(after.health()).isEqualTo(9);
        assertThat(after.dead()).isFalse();
        assertThat(after.resources()).containsEntry("coin", 2);
        assertThat(after.inventory()).doesNotContain("knife").contains("rope", "lamp").doesNotContain("crown");
        assertThat(after.turn()).isEqualTo(1);
        StoryNode node = after.nodes().getLast();
        assertThat(node.action()).isEqualTo("I walk to the door");
        assertThat(node.rollText()).isEqualTo("no roll");
    }

    @Test
    void rolledActionsRecordTheCheck() {
        GameSession session = create();
        provider.plan = new ActionPlan(true, "", true, "Stealth", 40, 9);

        GameSession after = gameService.act(session.id(), new ActionRequest("I sneak past the guard"));

        assertThat(provider.lastRoll).isNotNull();
        assertThat(provider.lastRoll.difficulty()).isEqualTo(25);
        assertThat(provider.lastRoll.modifier()).isEqualTo(3);
        assertThat(after.nodes().getLast().rollText()).startsWith("Stealth: d20 ");
    }

    @Test
    void invalidActionsDoNotAdvanceTheStory() {
        GameSession session = create();
        provider.plan = new ActionPlan(false, "That is not possible here.", false, "", 0, 0);

        GameSession after = gameService.act(session.id(), new ActionRequest("I summon a dragon"));

        assertThat(after.turn()).isZero();
        assertThat(after.nodes().getLast().type()).isEqualTo(StoryNode.REJECTED);
        assertThat(after.nodes().getLast().text()).isEqualTo("That is not possible here.");
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

    private GameSession create() {
        return gameService.create(new CreateSessionRequest("Mara", "smuggler", "dark-fantasy", "en"));
    }

    private static TurnOutcome outcome(int health, boolean died, List<String> lost, List<String> gained, int coin) {
        return new TurnOutcome("Something happens.", "Old mill", health, 0, gained, lost, coin, 0, 0,
                List.of(), List.of(), died, List.of("x", "y", "z"), "image");
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

        @Override
        public SceneIntro intro(GameSession session) {
            return new SceneIntro("You arrive.", "Old mill", List.of("a", "b", "c"), "image");
        }

        @Override
        public ActionPlan plan(GameSession session, String action) {
            return plan;
        }

        @Override
        public TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll) {
            lastRoll = roll;
            return outcome;
        }
    }
}
