package de.lwlab.gptdnd.service.rules;

import de.lwlab.gptdnd.domain.Beat;
import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.StoryNode;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Decides the pacing of each turn. Left alone, a model falls into a safe pattern of walking and finding
 * things, so the backend keeps the threat moving and varies what kind of moment comes next.
 */
public final class StoryDirector {
    public static final int CLOCK_MAX = 6;
    private static final int CLOCK_WARNING = 3;
    private static final int CLOCK_AFTER_CLIMAX = 2;
    private static final int FIGURE_BY_TURN = 3;
    private static final int SCENERY_EVERY_TURNS = 5;
    private static final int COIN_COOLDOWN_STOPS = 3;

    private StoryDirector() {
    }

    public static Direction next(GameSession session, Degree degree, RandomGenerator random) {
        int before = session.threatClock();
        boolean failed = degree == Degree.FAILURE || degree == Degree.CRITICAL_FAILURE;
        int after = Math.min(CLOCK_MAX, before + (failed ? 2 : 1));
        boolean strikes = (before < CLOCK_WARNING && after >= CLOCK_WARNING) || after >= CLOCK_MAX;
        int upcomingTurn = session.turn() + 1;
        return new Direction(
                beat(session, failed, strikes, random),
                after >= CLOCK_MAX ? CLOCK_AFTER_CLIMAX : after,
                strikes,
                session.characters().isEmpty() && upcomingTurn >= FIGURE_BY_TURN,
                upcomingTurn % SCENERY_EVERY_TURNS == 0,
                !coinsFoundRecently(session));
    }

    private static Beat beat(GameSession session, boolean failed, boolean strikes, RandomGenerator random) {
        if (strikes) {
            return Beat.ENCOUNTER;
        }
        List<Beat> recent = session.recentBeats();
        Beat last = recent.isEmpty() ? null : recent.getLast();
        if (failed && last != Beat.COMPLICATION) {
            return Beat.COMPLICATION;
        }
        if (session.health() <= 3 && last != Beat.BREATHER) {
            return Beat.BREATHER;
        }
        List<Beat> options = new ArrayList<>(List.of(Beat.values()));
        options.remove(last);
        // Beats not seen for a while stay in the running, recent ones get pushed out first.
        if (options.size() > 2) {
            options.removeAll(recent);
        }
        if (options.isEmpty()) {
            options = new ArrayList<>(List.of(Beat.values()));
            options.remove(last);
        }
        return options.get(random.nextInt(options.size()));
    }

    private static boolean coinsFoundRecently(GameSession session) {
        List<StoryNode> stops = session.nodes().stream().filter(node -> StoryNode.STORY.equals(node.type())).toList();
        return stops.subList(Math.max(0, stops.size() - COIN_COOLDOWN_STOPS), stops.size()).stream()
                .flatMap(node -> node.consequences().stream())
                .anyMatch(Consequence::foundCoins);
    }
}
