package de.lwlab.gptdnd.service.rules;

import static org.assertj.core.api.Assertions.assertThat;

import de.lwlab.gptdnd.domain.Beat;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.StoryNode;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StoryDirectorTest {
    private final Random random = new Random(7);

    @Test
    void threatClockAdvancesEachTurnAndFasterOnFailure() {
        GameSession session = session();

        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).threatClock()).isEqualTo(1);
        assertThat(StoryDirector.next(session, Degree.FAILURE, random).threatClock()).isEqualTo(2);
    }

    @Test
    void threatStrikesAtThresholdsAndForcesAnEncounter() {
        GameSession session = session();
        session.threatClock(2);
        Direction warning = StoryDirector.next(session, Degree.SUCCESS, random);
        assertThat(warning.threatStrikes()).isTrue();
        assertThat(warning.beat()).isEqualTo(Beat.ENCOUNTER);

        session.threatClock(5);
        Direction climax = StoryDirector.next(session, Degree.AUTOMATIC_SUCCESS, random);
        assertThat(climax.threatStrikes()).isTrue();
        assertThat(climax.threatClock()).isEqualTo(2);

        session.threatClock(3);
        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).threatStrikes()).isFalse();
    }

    @Test
    void neverRepeatsTheSameBeatTwiceInARow() {
        GameSession session = session();
        for (Beat last : Beat.values()) {
            session.recentBeats().clear();
            session.recentBeats().add(last);
            for (int i = 0; i < 50; i++) {
                assertThat(StoryDirector.next(session, Degree.SUCCESS, random).beat()).isNotEqualTo(last);
            }
        }
    }

    @Test
    void failureLeadsToAComplication() {
        assertThat(StoryDirector.next(session(), Degree.FAILURE, random).beat()).isEqualTo(Beat.COMPLICATION);
    }

    @Test
    void requiresAFigureByTurnThreeUnlessOneExists() {
        GameSession session = session();
        session.incrementTurn();
        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).introduceFigure()).isFalse();
        session.incrementTurn();
        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).introduceFigure()).isTrue();
        session.characters().add("Ada, a scavenger");
        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).introduceFigure()).isFalse();
    }

    @Test
    void coinsAreOnCooldownAfterARecentFind() {
        GameSession session = session();
        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).coinsAllowed()).isTrue();

        session.nodes().add(new StoryNode("n1", StoryNode.STORY, "a", "t", "no roll", null, null,
                List.of(new Consequence("+4 coin", Consequence.Tone.GOOD)), Instant.now()));

        assertThat(StoryDirector.next(session, Degree.SUCCESS, random).coinsAllowed()).isFalse();
    }

    private static GameSession session() {
        return new GameSession("s", new CharacterProfile("Gustav", "mechanic", "wasteland"), Instant.now());
    }
}
