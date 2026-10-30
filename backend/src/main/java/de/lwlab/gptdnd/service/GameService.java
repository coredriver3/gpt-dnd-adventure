package de.lwlab.gptdnd.service;

import de.lwlab.gptdnd.api.ActionRequest;
import de.lwlab.gptdnd.api.CreateSessionRequest;
import de.lwlab.gptdnd.api.PreferenceRequest;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.GameSetting;
import de.lwlab.gptdnd.domain.Language;
import de.lwlab.gptdnd.domain.StoryNode;
import de.lwlab.gptdnd.service.rules.Degree;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.story.ActionPlan;
import de.lwlab.gptdnd.service.story.SceneIntro;
import de.lwlab.gptdnd.service.story.StoryProvider;
import de.lwlab.gptdnd.service.story.TurnOutcome;
import de.lwlab.gptdnd.store.GameSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private static final int MAX_EFFECTS = 6;
    private static final int MAX_INVENTORY = 16;

    private final GameSessionRepository repository;
    private final StoryProvider storyProvider;

    public GameService(GameSessionRepository repository, StoryProvider storyProvider) {
        this.repository = repository;
        this.storyProvider = storyProvider;
    }

    public List<GameSetting> settings() {
        return List.of(GameSetting.values());
    }

    public GameSession create(CreateSessionRequest request) {
        GameSetting setting = GameSetting.fromId(request.setting());
        CharacterProfile character = new CharacterProfile(clean(request.name(), "Mara"), clean(request.concept(), "traveller"), setting);
        GameSession session = new GameSession(UUID.randomUUID().toString(), character, Instant.now());
        if (request.language() != null) {
            session.language(Language.valueOf(request.language().toUpperCase()));
        }
        SceneIntro intro = storyProvider.intro(session);
        session.location(text(intro.location(), 200));
        replaceSuggestions(session, intro.suggestedActions());
        addNode(session, StoryNode.SCENE, null, intro.narration(), "no roll", intro.imagePrompt(), List.of(
                new Consequence("inventory tracked", Consequence.Tone.NEUTRAL),
                new Consequence("death enabled", Consequence.Tone.BAD),
                new Consequence("contextual dice difficulty", Consequence.Tone.NEUTRAL)));
        return repository.save(session);
    }

    /** All sessions, most recently played first. */
    public List<GameSession> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(GameSession::lastPlayedAt).reversed())
                .toList();
    }

    public GameSession get(String id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Session not found"));
    }

    public GameSession applyPreferences(String id, PreferenceRequest request) {
        GameSession session = get(id);
        if (request.language() != null) {
            session.language(Language.valueOf(request.language().toUpperCase()));
        }
        if (request.images() != null) {
            session.images(request.images());
        }
        return repository.save(session);
    }

    public GameSession act(String id, ActionRequest request) {
        GameSession session = get(id);
        String text = clean(request.text(), "");
        if (session.dead()) {
            addNode(session, StoryNode.SYSTEM, text, "This character is dead. Start a new game.", "no roll", null,
                    List.of(new Consequence("fatal state", Consequence.Tone.BAD)));
            return repository.save(session);
        }
        if (text.matches("^\\[.*]$")) {
            runCommand(session, text);
        } else if (text.length() < 2) {
            reject(session, text, "Input rejected: describe what your character does.");
        } else {
            narrate(session, text);
        }
        return repository.save(session);
    }

    private void narrate(GameSession session, String text) {
        ActionPlan plan = storyProvider.plan(session, text);
        if (!plan.valid()) {
            reject(session, text, clean(plan.rejection(), "Input rejected: that action does not fit the current scene."));
            return;
        }
        RollResult roll = null;
        if (plan.rollNeeded()) {
            int difficulty = clamp(plan.difficulty(), 5, 25);
            int modifier = clamp(plan.modifier(), -2, 3);
            int natural = ThreadLocalRandom.current().nextInt(1, 21);
            roll = new RollResult(clean(plan.skill(), "Check"), natural, modifier, difficulty,
                    Degree.of(natural, natural + modifier, difficulty));
        }
        TurnOutcome outcome = storyProvider.resolve(session, text, plan, roll);
        Degree degree = roll == null ? Degree.NO_ROLL : roll.degree();
        List<Consequence> consequences = apply(session, outcome, degree);
        consequences.add(new Consequence(degree.label(), switch (degree) {
            case SUCCESS, CRITICAL_SUCCESS -> Consequence.Tone.GOOD;
            case FAILURE, CRITICAL_FAILURE -> Consequence.Tone.BAD;
            default -> Consequence.Tone.NEUTRAL;
        }));
        addNode(session, StoryNode.STORY, text, outcome.narration(), roll == null ? "no roll" : roll.describe(),
                outcome.imagePrompt(), consequences);
    }

    /** Applies the narrator's proposed changes, clamped to what the rolled degree allows. */
    private List<Consequence> apply(GameSession session, TurnOutcome outcome, Degree degree) {
        List<Consequence> consequences = new ArrayList<>();
        session.incrementTurn();
        if (outcome.location() != null && !outcome.location().isBlank()) {
            session.location(text(outcome.location(), 200));
        }
        replaceSuggestions(session, outcome.suggestedActions());

        int health = degree.clampHealth(outcome.healthDelta());
        int resolve = degree.clampResolve(outcome.resolveDelta());
        session.health(session.health() + health);
        session.resolve(session.resolve() + resolve);
        if (health != 0) {
            consequences.add(new Consequence(signed(health) + " health", health > 0 ? Consequence.Tone.GOOD : Consequence.Tone.BAD));
        }
        if (resolve != 0) {
            consequences.add(new Consequence(signed(resolve) + " resolve", resolve > 0 ? Consequence.Tone.GOOD : Consequence.Tone.BAD));
        }

        changeResource(session, "coin", clamp(outcome.coinDelta(), -10, 10), consequences);
        changeResource(session, "rations", clamp(outcome.rationsDelta(), -2, 2), consequences);
        changeResource(session, "torch", clamp(outcome.torchDelta(), -2, 2), consequences);

        for (String item : limit(outcome.itemsLost(), 3)) {
            session.inventory().stream()
                    .filter(owned -> owned.equalsIgnoreCase(item))
                    .findFirst()
                    .ifPresent(owned -> {
                        session.inventory().remove(owned);
                        consequences.add(new Consequence("lost " + owned, Consequence.Tone.BAD));
                    });
        }
        for (String item : limit(outcome.itemsGained(), 2)) {
            if (session.inventory().size() < MAX_INVENTORY) {
                session.inventory().add(text(item, 60));
                consequences.add(new Consequence("gained " + text(item, 60), Consequence.Tone.GOOD));
            }
        }

        for (String effect : limit(outcome.effectsRemoved(), MAX_EFFECTS)) {
            if (session.effects().removeIf(existing -> existing.equalsIgnoreCase(effect))) {
                consequences.add(new Consequence("no longer " + effect, Consequence.Tone.GOOD));
            }
        }
        for (String effect : limit(outcome.effectsAdded(), 2)) {
            String value = text(effect, 60);
            if (session.effects().size() < MAX_EFFECTS && session.effects().stream().noneMatch(value::equalsIgnoreCase)) {
                session.effects().add(value);
                consequences.add(new Consequence(value, Consequence.Tone.BAD));
            }
        }

        if (outcome.characterDied() && degree.allowsDeath()) {
            session.health(0);
        }
        if (session.health() == 0) {
            session.dead(true);
            session.suggestedActions().clear();
            consequences.add(new Consequence("character died", Consequence.Tone.BAD));
        }
        return consequences;
    }

    private void changeResource(GameSession session, String key, int delta, List<Consequence> consequences) {
        int current = session.resources().getOrDefault(key, 0);
        int next = Math.max(0, current + delta);
        if (next != current) {
            session.resources().put(key, next);
            consequences.add(new Consequence(signed(next - current) + " " + key,
                    next > current ? Consequence.Tone.GOOD : Consequence.Tone.BAD));
        }
    }

    private void reject(GameSession session, String text, String reason) {
        addNode(session, StoryNode.REJECTED, text, reason, "no roll", null,
                List.of(new Consequence("story did not advance", Consequence.Tone.BAD)));
    }

    private void runCommand(GameSession session, String text) {
        String command = text.substring(1, text.length() - 1).trim().toLowerCase();
        if (command.startsWith("roll")) {
            int sides = 20;
            int mod = 0;
            var matcher = java.util.regex.Pattern.compile("d(\\d+)([+-]\\d+)?").matcher(command);
            if (matcher.find()) {
                sides = clamp(Integer.parseInt(matcher.group(1)), 2, 100);
                mod = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            }
            int total = ThreadLocalRandom.current().nextInt(1, sides + 1) + mod;
            addNode(session, StoryNode.COMMAND, text, "Manual roll: d%s%s = %s. The story does not advance."
                            .formatted(sides, mod == 0 ? "" : signed(mod), total),
                    String.valueOf(total), null, List.of(new Consequence("command mode", Consequence.Tone.NEUTRAL)));
            return;
        }

        Map<String, String> reports = Map.of(
                "inventory", "Inventory: " + String.join(", ", session.inventory()) + ".",
                "status", "Health %s/%s, Resolve %s/%s. Location: %s. Effects: %s.".formatted(
                        session.health(), session.maxHealth(), session.resolve(), session.maxResolve(),
                        session.location().isBlank() ? "unknown" : session.location(),
                        session.effects().isEmpty() ? "none" : String.join(", ", session.effects())),
                "help", "Commands: [inventory], [status], [roll d20+2], [image on], [image off].",
                "image on", "Scene image cards enabled.",
                "image off", "Scene image cards disabled.");
        if (Objects.equals(command, "image on")) {
            session.images(true);
        }
        if (Objects.equals(command, "image off")) {
            session.images(false);
        }
        addNode(session, StoryNode.COMMAND, text, reports.getOrDefault(command, "Unknown command. Try [help]. The story does not advance."),
                "no roll", null, List.of(new Consequence("no story advance", Consequence.Tone.NEUTRAL)));
    }

    private void replaceSuggestions(GameSession session, List<String> suggestions) {
        session.suggestedActions().clear();
        limit(suggestions, 3).forEach(suggestion -> session.suggestedActions().add(text(suggestion, 200)));
    }

    private void addNode(GameSession session, String type, String action, String text, String rollText, String imagePrompt,
            List<Consequence> consequences) {
        session.nodes().add(new StoryNode(UUID.randomUUID().toString(), type, action, text(text, 5000), rollText,
                imagePrompt == null ? null : text(imagePrompt, 1000), consequences, Instant.now()));
    }

    private static List<String> limit(List<String> values, int max) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .limit(max)
                .toList();
    }

    private static String text(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
