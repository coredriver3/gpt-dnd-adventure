package de.lwlab.gptdnd.service;

import de.lwlab.gptdnd.api.ActionRequest;
import de.lwlab.gptdnd.api.CreateSessionRequest;
import de.lwlab.gptdnd.api.PreferenceRequest;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.DiceRoll;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.GameSetting;
import de.lwlab.gptdnd.domain.Language;
import de.lwlab.gptdnd.domain.StoryNode;
import de.lwlab.gptdnd.service.rules.Degree;
import de.lwlab.gptdnd.service.rules.Direction;
import de.lwlab.gptdnd.service.rules.RollResult;
import de.lwlab.gptdnd.service.rules.StartingGear;
import de.lwlab.gptdnd.service.rules.StoryDirector;
import de.lwlab.gptdnd.service.story.ActionPlan;
import de.lwlab.gptdnd.service.story.SceneIntro;
import de.lwlab.gptdnd.service.story.StoryProvider;
import de.lwlab.gptdnd.service.story.StoryProviderException;
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
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private static final int MAX_EFFECTS = 6;
    private static final int MAX_INVENTORY = 16;
    private static final int MAX_CHARACTERS = 8;
    private static final int MAX_THREADS = 4;
    private static final int BEAT_MEMORY = 3;
    private static final int MAX_COINS_FOUND = 6;
    private static final Pattern VALID_CHARACTER = Pattern.compile("\\p{Lu}[^\\n/]{2,200}");
    private static final Pattern VALID_THREAD = Pattern.compile("\\p{L}[^\\n/]{9,200}");
    private static final Pattern TECHNICAL_TEXT = Pattern.compile(
            "\\b(healthDelta|coinDelta|characterDied|itemsLost|itemsGained|effectsAdded"
                    + "|effectsRemoved|suggestedActions|imagePrompt|openThreads)\\b");

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
        CharacterProfile character = new CharacterProfile(request.name().trim(), request.concept().trim(), request.setting().trim());
        GameSession session = new GameSession(UUID.randomUUID().toString(), character, Instant.now());
        if (request.language() != null) {
            session.language(Language.valueOf(request.language().toUpperCase()));
        }
        StartingGear gear = StartingGear.of(rollD20());
        SceneIntro intro = storyProvider.intro(session, gear);
        session.location(text(intro.location(), 200));
        updateStoryState(session, intro.goal(), intro.threat(), intro.layout(), intro.characters(), intro.openThreads());
        replaceSuggestions(session, intro.suggestedActions());
        List<Consequence> consequences = new ArrayList<>();
        for (String item : limit(intro.startingItems(), gear.itemCount())) {
            session.inventory().add(text(item, 60));
            consequences.add(new Consequence("gained " + text(item, 60), Consequence.Tone.GOOD));
        }
        DiceRoll roll = new DiceRoll("Starting gear", gear.natural(), 0, null, gear.outcome(), gear.tone());
        addNode(session, StoryNode.SCENE, null, cleanNarration(intro.narration()), "Starting gear: d20 %d / %s".formatted(gear.natural(), gear.outcome()),
                roll, intro.imagePrompt(), consequences);
        return repository.save(session);
    }

    /** All sessions, most recently played first. */
    public List<GameSession> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(GameSession::lastPlayedAt).reversed())
                .toList();
    }

    public void delete(String id) {
        get(id);
        repository.deleteById(id);
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
            addNode(session, StoryNode.SYSTEM, text, "This character is dead. Start a new game.", "no roll", null, null,
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
        if (plan.decision() == ActionPlan.Decision.INVALID) {
            reject(session, text, clean(plan.rejection(), "Input rejected: that action does not fit the current scene."));
            return;
        }
        RollResult roll = null;
        Degree degree = plan.decision() == ActionPlan.Decision.AUTOMATIC_FAILURE ? Degree.AUTOMATIC_FAILURE : Degree.AUTOMATIC_SUCCESS;
        if (plan.decision() == ActionPlan.Decision.ROLL) {
            int difficulty = clamp(plan.difficulty(), 5, 25);
            int modifier = clamp(plan.modifier(), -2, 3);
            int natural = rollD20();
            degree = Degree.of(natural, natural + modifier, difficulty);
            roll = new RollResult(clean(plan.skill(), "Check"), natural, modifier, difficulty, degree);
        }
        Direction direction = StoryDirector.next(session, degree, ThreadLocalRandom.current());
        TurnOutcome outcome = storyProvider.resolve(session, text, plan, roll, direction);
        if (TECHNICAL_TEXT.matcher(outcome.narration()).find()) {
            outcome = storyProvider.resolve(session, text, plan, roll, direction);
        }
        Consequence.Tone tone = switch (degree) {
            case SUCCESS, CRITICAL_SUCCESS -> Consequence.Tone.GOOD;
            case FAILURE, CRITICAL_FAILURE, AUTOMATIC_FAILURE -> Consequence.Tone.BAD;
            default -> Consequence.Tone.NEUTRAL;
        };
        DiceRoll dice = roll == null ? null
                : new DiceRoll(roll.skill(), roll.natural(), roll.modifier(), roll.difficulty(), degree.label(), tone);
        List<Consequence> consequences = apply(session, outcome, degree, direction);
        consequences.add(new Consequence(degree.label(), tone));
        String narration = cleanNarration(outcome.narration());
        addNode(session, StoryNode.STORY, text, session.dead() ? deathScene(session, text, narration) : narration, roll == null ? "no roll" : roll.describe(),
                dice, outcome.imagePrompt(), consequences);
    }

    /** If this extra request fails, the turn still counts and keeps its original text. */
    private String deathScene(GameSession session, String action, String narration) {
        try {
            String scene = cleanNarration(storyProvider.death(session, action, narration).narration());
            return scene.isBlank() ? narration : scene;
        } catch (StoryProviderException exception) {
            return narration;
        }
    }

    /** Models sometimes spill structured fields or stray markup into the prose; that never reaches the player. */
    private static String cleanNarration(String narration) {
        return narration.lines()
                .filter(line -> !TECHNICAL_TEXT.matcher(line).find())
                .collect(java.util.stream.Collectors.joining("\n"))
                .replaceFirst("^[\\s>#*]+", "")
                .trim();
    }

    /** Keeps the current list unless the model delivered usable entries; weak models fill these fields with garbage. */
    private static void replaceStoryList(List<String> current, List<String> proposed, int max, Pattern valid) {
        List<String> usable = limit(proposed, Integer.MAX_VALUE).stream()
                .filter(entry -> valid.matcher(entry).matches())
                .limit(max)
                .toList();
        if (!usable.isEmpty()) {
            current.clear();
            usable.forEach(entry -> current.add(text(entry, 300)));
        }
    }

    private void updateStoryState(GameSession session, String goal, String threat, String layout, List<String> characters, List<String> openThreads) {
        if (goal != null && !goal.isBlank()) {
            session.goal(text(goal, 500));
        }
        if (threat != null && !threat.isBlank()) {
            session.threat(text(threat, 500));
        }
        if (layout != null && !layout.isBlank()) {
            session.layout(text(layout, 1000));
        }
        replaceStoryList(session.characters(), characters, MAX_CHARACTERS, VALID_CHARACTER);
        replaceStoryList(session.openThreads(), openThreads, MAX_THREADS, VALID_THREAD);
    }

    /** Applies the narrator's proposed changes, clamped to what the rolled degree allows. */
    private List<Consequence> apply(GameSession session, TurnOutcome outcome, Degree degree, Direction direction) {
        List<Consequence> consequences = new ArrayList<>();
        session.incrementTurn();
        if (outcome.location() != null && !outcome.location().isBlank()) {
            session.location(text(outcome.location(), 200));
        }
        replaceSuggestions(session, outcome.suggestedActions());
        updateStoryState(session, outcome.goal(), outcome.threat(), outcome.layout(), outcome.characters(), outcome.openThreads());
        session.threatClock(direction.threatClock());
        session.recentBeats().add(direction.beat());
        if (session.recentBeats().size() > BEAT_MEMORY) {
            session.recentBeats().removeFirst();
        }

        int health = degree.clampHealth(outcome.healthDelta(), direction.threatStrikes());
        session.health(session.health() + health);
        if (health != 0) {
            consequences.add(new Consequence(signed(health) + " health", health > 0 ? Consequence.Tone.GOOD : Consequence.Tone.BAD));
        }

        changeResource(session, "coin", clamp(outcome.coinDelta(), Integer.MIN_VALUE, direction.coinsAllowed() ? MAX_COINS_FOUND : 0), consequences);

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

        // A cost the model forgot never kills on its own.
        if (degree.requiresCost() && session.health() > 1 && consequences.stream().noneMatch(c -> c.tone() == Consequence.Tone.BAD)) {
            session.health(session.health() - 1);
            consequences.add(new Consequence("-1 health", Consequence.Tone.BAD));
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
        addNode(session, StoryNode.REJECTED, text, reason, "no roll", null, null,
                List.of(new Consequence("story did not advance", Consequence.Tone.BAD)));
    }

    private void runCommand(GameSession session, String text) {
        String command = text.substring(1, text.length() - 1).trim().toLowerCase();
        Map<String, String> reports = Map.of(
                "inventory", "Inventory: " + String.join(", ", session.inventory()) + ".",
                "status", "Health %s/%s. Location: %s. Effects: %s.".formatted(
                        session.health(), session.maxHealth(),
                        session.location().isBlank() ? "unknown" : session.location(),
                        session.effects().isEmpty() ? "none" : String.join(", ", session.effects())),
                "help", "Commands: [inventory], [status], [image on], [image off].",
                "image on", "Scene image cards enabled.",
                "image off", "Scene image cards disabled.");
        if (Objects.equals(command, "image on")) {
            session.images(true);
        }
        if (Objects.equals(command, "image off")) {
            session.images(false);
        }
        addNode(session, StoryNode.COMMAND, text, reports.getOrDefault(command, "Unknown command. Try [help]. The story does not advance."),
                "no roll", null, null, List.of(new Consequence("no story advance", Consequence.Tone.NEUTRAL)));
    }

    private void replaceSuggestions(GameSession session, List<String> suggestions) {
        session.suggestedActions().clear();
        limit(suggestions, 3).forEach(suggestion -> session.suggestedActions().add(text(suggestion, 200)));
    }

    private void addNode(GameSession session, String type, String action, String text, String rollText, DiceRoll roll, String imagePrompt,
            List<Consequence> consequences) {
        session.nodes().add(new StoryNode(UUID.randomUUID().toString(), type, action, text(text, 5000), rollText, roll,
                imagePrompt == null ? null : text(imagePrompt, 1000), consequences, Instant.now()));
    }

    private static int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    private static List<String> limit(List<String> values, int max) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .limit(max)
                .toList();
    }

    private static String text(String value, int maxLength) {
        // Models tend to keep using dashes as punctuation despite the prompt.
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s*[\u2014\u2013]\\s*|\\s+-\\s+", ", ");
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
