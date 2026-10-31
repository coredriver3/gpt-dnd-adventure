package de.lwlab.gptdnd.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameSession {
    private final String id;
    private final CharacterProfile character;
    private final Instant createdAt;
    private Language language = Language.DE;
    private boolean images = true;
    private boolean dead = false;
    private int turn = 0;
    private int health = 10;
    private int maxHealth = 10;
    private int resolve = 6;
    private int maxResolve = 6;
    private String location = "";
    private final Map<String, Integer> resources = new LinkedHashMap<>();
    private final List<String> inventory = new ArrayList<>();
    private final List<String> effects = new ArrayList<>();
    private final List<StoryNode> nodes = new ArrayList<>();
    private final List<String> suggestedActions = new ArrayList<>();

    public GameSession(String id, CharacterProfile character, Instant createdAt) {
        this.id = id;
        this.character = character;
        this.createdAt = createdAt;
        resources.put("coin", 12);
        resources.put("rations", 3);
        resources.put("torch", 2);
    }

    public String id() {
        return id;
    }

    public CharacterProfile character() {
        return character;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Language language() {
        return language;
    }

    public void language(Language language) {
        this.language = language;
    }

    public boolean images() {
        return images;
    }

    public void images(boolean images) {
        this.images = images;
    }

    public boolean dead() {
        return dead;
    }

    public void dead(boolean dead) {
        this.dead = dead;
    }

    public int turn() {
        return turn;
    }

    public void incrementTurn() {
        this.turn += 1;
    }

    public int health() {
        return health;
    }

    public void health(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int resolve() {
        return resolve;
    }

    public void resolve(int resolve) {
        this.resolve = Math.max(0, Math.min(maxResolve, resolve));
    }

    public int maxResolve() {
        return maxResolve;
    }

    public Map<String, Integer> resources() {
        return resources;
    }

    public List<String> inventory() {
        return inventory;
    }

    public List<String> effects() {
        return effects;
    }

    public List<StoryNode> nodes() {
        return nodes;
    }

    public String location() {
        return location;
    }

    public void location(String location) {
        this.location = location == null ? "" : location;
    }

    public List<String> suggestedActions() {
        return suggestedActions;
    }

    public Instant lastPlayedAt() {
        return nodes.isEmpty() ? createdAt : nodes.getLast().createdAt();
    }
}
