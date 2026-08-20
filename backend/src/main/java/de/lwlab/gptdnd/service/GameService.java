package de.lwlab.gptdnd.service;

import de.lwlab.gptdnd.api.ActionRequest;
import de.lwlab.gptdnd.api.CreateSessionRequest;
import de.lwlab.gptdnd.api.PreferenceRequest;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.GameSetting;
import de.lwlab.gptdnd.domain.Language;
import de.lwlab.gptdnd.domain.RollOutcome;
import de.lwlab.gptdnd.domain.StoryNode;
import de.lwlab.gptdnd.service.story.StoryDraft;
import de.lwlab.gptdnd.service.story.StoryPrompt;
import de.lwlab.gptdnd.service.story.StoryProvider;
import de.lwlab.gptdnd.store.GameSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private static final List<RollOutcome> OUTCOMES = List.of(
            new RollOutcome(1, 4, "critical failure", -4, -1),
            new RollOutcome(5, 8, "failure", -2, 0),
            new RollOutcome(9, 12, "mixed result", -1, -1),
            new RollOutcome(13, 16, "success with cost", 0, -1),
            new RollOutcome(17, 19, "clean success", 0, 0),
            new RollOutcome(20, 20, "critical success", 0, 1));

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
        addIntro(session);
        return repository.save(session);
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
            addNode(session, "system", "This character is dead. Start a new game.", "no roll", null,
                    List.of(new Consequence("fatal state", Consequence.Tone.BAD)));
            return repository.save(session);
        }
        if (!validInput(text)) {
            addNode(session, "rejected input",
                    "Input rejected: use coherent German or English actions that fit the current scene. Commands belong in square brackets.",
                    "no roll", null, List.of(new Consequence("story did not advance", Consequence.Tone.BAD)));
            return repository.save(session);
        }
        if (text.matches("^\\[.*]$")) {
            runCommand(session, text);
        } else {
            narrate(session, text);
        }
        return repository.save(session);
    }

    private void addIntro(GameSession session) {
        String place = randomPlace(session);
        StoryDraft draft = storyProvider.intro(new StoryPrompt(session, "", place, null,
                new RollOutcome(0, 0, "scene", 0, 0), false));
        addNode(session, "scene", draft.text(), "no roll", draft.imagePrompt(), List.of(
                new Consequence("inventory tracked", Consequence.Tone.NEUTRAL),
                new Consequence("death enabled", Consequence.Tone.BAD),
                new Consequence("contextual dice difficulty", Consequence.Tone.NEUTRAL)));
    }

    private void narrate(GameSession session, String text) {
        String place = randomPlace(session);
        int danger = Math.min(4, Math.floorDiv(session.turn(), 3) + (session.health() <= 4 ? 1 : 0));
        Integer natural = needsRoll(text) ? roll(20, 0) : null;
        RollOutcome outcome = natural == null
                ? new RollOutcome(0, 0, "information gained", 0, 0)
                : outcomeFor(Math.max(1, Math.min(20, natural - danger)));
        List<Consequence> consequences = new ArrayList<>();

        session.incrementTurn();
        session.health(session.health() + outcome.healthDelta());
        session.resolve(session.resolve() + outcome.resolveDelta());

        if (natural != null && natural >= 17 && chance(58)) {
            String reward = random(List.of("silver token", "clean bandage", "sealed clue", "warm meal"));
            session.inventory().add(reward);
            consequences.add(new Consequence("gained " + reward, Consequence.Tone.GOOD));
        }
        if (natural != null && natural <= 8 && chance(65)) {
            session.resources().entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .findFirst()
                    .ifPresent(entry -> {
                        session.resources().put(entry.getKey(), entry.getValue() - 1);
                        consequences.add(new Consequence("lost 1 " + entry.getKey(), Consequence.Tone.BAD));
                    });
        }
        if (session.health() == 0) {
            session.dead(true);
            consequences.add(new Consequence("character died", Consequence.Tone.BAD));
        }
        if (outcome.healthDelta() < 0) {
            consequences.add(new Consequence(outcome.healthDelta() + " health", Consequence.Tone.BAD));
        }
        if (outcome.resolveDelta() < 0) {
            consequences.add(new Consequence(outcome.resolveDelta() + " resolve", Consequence.Tone.BAD));
        }
        if (outcome.resolveDelta() > 0) {
            consequences.add(new Consequence("+" + outcome.resolveDelta() + " resolve", Consequence.Tone.GOOD));
        }
        consequences.add(new Consequence(outcome.grade(), natural != null && natural >= 17
                ? Consequence.Tone.GOOD
                : natural != null && natural <= 8 ? Consequence.Tone.BAD : Consequence.Tone.NEUTRAL));

        StoryDraft draft = storyProvider.action(new StoryPrompt(session, text, place, natural, outcome, false));
        String rollText = natural == null ? "no roll" : "d20 " + natural + " / " + outcome.grade();
        addNode(session, "story point", draft.text(), rollText, draft.imagePrompt(), consequences);
    }

    private void runCommand(GameSession session, String text) {
        String command = text.substring(1, text.length() - 1).trim().toLowerCase();
        if (command.startsWith("roll")) {
            int sides = 20;
            int mod = 0;
            var matcher = java.util.regex.Pattern.compile("d(\\d+)([+-]\\d+)?").matcher(command);
            if (matcher.find()) {
                sides = Integer.parseInt(matcher.group(1));
                mod = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            }
            int total = roll(sides, mod);
            addNode(session, "command", "Manual roll: d%s%s = %s. The story does not advance."
                            .formatted(sides, mod == 0 ? "" : mod > 0 ? "+" + mod : String.valueOf(mod), total),
                    String.valueOf(total), null, List.of(new Consequence("command mode", Consequence.Tone.NEUTRAL)));
            return;
        }

        Map<String, String> reports = Map.of(
                "inventory", "Inventory: " + String.join(", ", session.inventory()) + ".",
                "status", "Health %s/%s, Resolve %s/%s. Effects: %s.".formatted(
                        session.health(), session.maxHealth(), session.resolve(), session.maxResolve(),
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
        addNode(session, "command", reports.getOrDefault(command, "Unknown command. Try [help]. The story does not advance."),
                "no roll", null, List.of(new Consequence("no story advance", Consequence.Tone.NEUTRAL)));
    }

    private void addNode(GameSession session, String type, String text, String rollText, String imagePrompt, List<Consequence> consequences) {
        session.nodes().add(new StoryNode(UUID.randomUUID().toString(), type, text, rollText, imagePrompt, consequences, Instant.now()));
    }

    private RollOutcome outcomeFor(int total) {
        return OUTCOMES.stream().filter(outcome -> outcome.matches(total)).findFirst().orElse(OUTCOMES.get(2));
    }

    private boolean needsRoll(String text) {
        return !text.matches("(?i)^(look|listen|warte|schaue|untersuche vorsichtig|inspect|observe)$");
    }

    private boolean validInput(String text) {
        if (text.length() < 2 || text.length() > 500) {
            return false;
        }
        if (text.matches("(?i).*(https?:|<script|drop table|ignore previous).*")) {
            return false;
        }
        if (!text.startsWith("[") && text.split("\\s+").length < 2) {
            return false;
        }
        return text.matches("[\\p{L}\\p{N}\\s.,!?;:'\"()\\[\\]/+\\-äöüÄÖÜß]+");
    }

    private String randomPlace(GameSession session) {
        return random(session.character().setting().places());
    }

    private String random(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private int roll(int sides, int mod) {
        return ThreadLocalRandom.current().nextInt(1, sides + 1) + mod;
    }

    private boolean chance(int percent) {
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }

    private String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
