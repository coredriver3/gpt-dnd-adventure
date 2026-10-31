package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.GameSetting;
import de.lwlab.gptdnd.domain.Language;
import de.lwlab.gptdnd.service.rules.Degree;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Deterministic, offline narrator used when no Claude API key is configured. */
public class LocalStoryProvider implements StoryProvider {
    private static final List<String> STARTING_ITEMS = List.of("weathered map", "knife", "ink kit", "rope", "flint and steel", "waterskin");
    private static final List<String> GENERIC_PLACES = List.of("a crossroads at dusk", "an abandoned waystation", "a narrow pass in the rain", "a half-empty market square");

    @Override
    public SceneIntro intro(GameSession session, StartingGear gear) {
        CharacterProfile character = session.character();
        String place = random(places(character.setting()));
        String text = session.language() == Language.DE
                ? "%s, %s, erreicht %s. Die Luft schmeckt nach Regen und Metall. Deine Vorräte sind knapp, und jede falsche Entscheidung kann bleibende Folgen haben."
                        .formatted(character.name(), character.concept(), place)
                : "%s, %s, reaches %s. The air tastes of rain and metal. Supplies are thin, and bad decisions can leave permanent scars."
                        .formatted(character.name(), character.concept(), place);
        List<String> items = new ArrayList<>(STARTING_ITEMS);
        Collections.shuffle(items);
        return new SceneIntro(text, place, items.subList(0, gear.itemCount()), suggestions(session.language()),
                "%s, %s, cinematic adventure concept art".formatted(character.setting(), place));
    }

    @Override
    public ActionPlan plan(GameSession session, String action) {
        if (!validInput(action)) {
            String rejection = session.language() == Language.DE
                    ? "Beschreibe eine zusammenhängende Handlung, die in die aktuelle Szene passt."
                    : "Describe a coherent action that fits the current scene.";
            return new ActionPlan(false, rejection, false, "", 0, 0);
        }
        boolean rollNeeded = !action.matches("(?i)^(look|listen|warte|schaue|untersuche vorsichtig|inspect|observe)$");
        int danger = Math.min(4, Math.floorDiv(session.turn(), 3) + (session.health() <= 4 ? 1 : 0));
        return new ActionPlan(true, "", rollNeeded, rollNeeded ? "Luck" : "", 10 + danger, 0);
    }

    @Override
    public TurnOutcome resolve(GameSession session, String action, ActionPlan plan, RollResult roll) {
        Degree degree = roll == null ? Degree.NO_ROLL : roll.degree();
        Language language = session.language();
        String place = random(places(session.character().setting()));
        String text = language == Language.DE
                ? "Du willst: \"%s\". In %s kippt die Lage in Richtung %s. %s".formatted(action, place, degree.label(), describe(degree, language))
                : "You attempt: \"%s\". At %s, the situation turns into a %s. %s".formatted(action, place, degree.label(), describe(degree, language));
        int health = switch (degree) {
            case CRITICAL_FAILURE -> -4;
            case FAILURE -> -2;
            case PARTIAL_SUCCESS -> -1;
            default -> 0;
        };
        int resolve = switch (degree) {
            case CRITICAL_FAILURE, PARTIAL_SUCCESS -> -1;
            case CRITICAL_SUCCESS -> 1;
            default -> 0;
        };
        boolean goodRoll = degree == Degree.SUCCESS || degree == Degree.CRITICAL_SUCCESS;
        boolean badRoll = degree == Degree.FAILURE || degree == Degree.CRITICAL_FAILURE;
        List<String> gained = goodRoll && chance(58)
                ? List.of(random(List.of("silver token", "clean bandage", "sealed clue", "warm meal")))
                : List.of();
        int rations = badRoll && chance(65) && session.resources().getOrDefault("rations", 0) > 0 ? -1 : 0;
        return new TurnOutcome(text, place, health, resolve, gained, List.of(), 0, rations, 0, List.of(), List.of(), false,
                suggestions(language), "%s, %s, player action: %s, %s, cinematic fantasy"
                        .formatted(session.character().setting(), place, action, degree.label()));
    }

    private List<String> places(String setting) {
        return GameSetting.find(setting).map(GameSetting::places).orElse(GENERIC_PLACES);
    }

    private String describe(Degree degree, Language language) {
        if (language == Language.EN) {
            return switch (degree) {
                case CRITICAL_FAILURE -> "The world takes something important from you.";
                case FAILURE -> "The attempt fails and forces you into a worse position.";
                case PARTIAL_SUCCESS -> "You reach your goal, yet you pay for it immediately.";
                case SUCCESS -> "You clearly prevail and take control of the moment.";
                case CRITICAL_SUCCESS -> "You dominate the scene, but the reward stays plausible.";
                case NO_ROLL -> "You gather detail without escalating the situation.";
            };
        }
        return switch (degree) {
            case CRITICAL_FAILURE -> "Die Welt nimmt dir etwas Wichtiges.";
            case FAILURE -> "Der Versuch scheitert und zwingt dich in eine schlechtere Position.";
            case PARTIAL_SUCCESS -> "Du erreichst dein Ziel, aber bezahlst sofort dafür.";
            case SUCCESS -> "Du setzt dich klar durch und gewinnst Kontrolle über den Moment.";
            case CRITICAL_SUCCESS -> "Du dominierst die Szene, aber die Belohnung bleibt plausibel.";
            case NO_ROLL -> "Du sammelst Details, ohne die Lage zu eskalieren.";
        };
    }

    private List<String> suggestions(Language language) {
        return language == Language.DE
                ? List.of("Schaue dich vorsichtig um", "Suche nach einem sicheren Unterschlupf", "Sprich die nächste Person an")
                : List.of("Look around carefully", "Search for a safe shelter", "Talk to the nearest person");
    }

    private boolean validInput(String text) {
        if (text.matches("(?i).*(https?:|<script|drop table|ignore previous).*")) {
            return false;
        }
        if (text.split("\\s+").length < 2) {
            return false;
        }
        return text.matches("[\\p{L}\\p{N}\\s.,!?;:'\"()/+\\-]+");
    }

    private String random(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private boolean chance(int percent) {
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }
}
