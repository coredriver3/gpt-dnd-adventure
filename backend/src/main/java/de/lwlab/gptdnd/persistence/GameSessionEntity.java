package de.lwlab.gptdnd.persistence;

import de.lwlab.gptdnd.domain.Beat;
import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.Language;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "game_sessions")
public class GameSessionEntity {
    @Id
    private String id;

    private String characterName;

    @Column(length = 500)
    private String characterConcept;

    private String settingId;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private Language language;

    private boolean images;
    private boolean dead;
    private int turnCount;
    private int health;
    private int maxHealth;

    @Column(length = 200)
    private String location;

    @Column(length = 500)
    private String storyGoal;

    @Column(length = 500)
    private String storyThreat;

    @Column(length = 1000)
    private String storyLayout;

    // Nullable: games from before the story state existed have no value yet.
    private Integer threatClock;

    private String recentBeats;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_story_characters", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "description", length = 300)
    private List<String> storyCharacters = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_open_threads", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "thread", length = 300)
    private List<String> openThreads = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_suggested_actions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "suggestion", length = 200)
    private List<String> suggestedActions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_resources", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "resource_key")
    @Column(name = "resource_value")
    private Map<String, Integer> resources = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_inventory", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "item")
    private List<String> inventory = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_effects", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "effect")
    private List<String> effects = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<StoryNodeEntity> nodes = new ArrayList<>();

    protected GameSessionEntity() {
    }

    public static GameSessionEntity fromDomain(GameSession session) {
        GameSessionEntity entity = new GameSessionEntity();
        entity.id = session.id();
        entity.characterName = session.character().name();
        entity.characterConcept = session.character().concept();
        entity.settingId = session.character().setting();
        entity.createdAt = session.createdAt();
        entity.language = session.language();
        entity.images = session.images();
        entity.dead = session.dead();
        entity.turnCount = session.turn();
        entity.health = session.health();
        entity.maxHealth = session.maxHealth();
        entity.resources = new LinkedHashMap<>(session.resources());
        entity.inventory = new ArrayList<>(session.inventory());
        entity.effects = new ArrayList<>(session.effects());
        entity.location = session.location();
        entity.suggestedActions = new ArrayList<>(session.suggestedActions());
        entity.storyGoal = session.goal();
        entity.storyThreat = session.threat();
        entity.storyLayout = session.layout();
        entity.threatClock = session.threatClock();
        entity.recentBeats = session.recentBeats().stream().map(Beat::name).collect(Collectors.joining(","));
        entity.storyCharacters = new ArrayList<>(session.characters());
        entity.openThreads = new ArrayList<>(session.openThreads());
        for (int i = 0; i < session.nodes().size(); i++) {
            entity.nodes.add(StoryNodeEntity.fromDomain(session.nodes().get(i), entity, i));
        }
        return entity;
    }

    public GameSession toDomain() {
        GameSession session = new GameSession(
                id,
                new CharacterProfile(characterName, characterConcept, settingId),
                createdAt);
        session.language(language);
        session.images(images);
        session.dead(dead);
        for (int i = 0; i < turnCount; i++) {
            session.incrementTurn();
        }
        session.health(health);
        // Older games also stored rations and torches; only coins still exist.
        session.resources().put("coin", resources.getOrDefault("coin", 0));
        session.inventory().clear();
        session.inventory().addAll(inventory);
        session.effects().clear();
        session.effects().addAll(effects);
        session.location(location);
        session.suggestedActions().addAll(suggestedActions);
        session.goal(storyGoal);
        session.threat(storyThreat);
        session.layout(storyLayout);
        session.threatClock(threatClock == null ? 0 : threatClock);
        if (recentBeats != null && !recentBeats.isBlank()) {
            Arrays.stream(recentBeats.split(",")).map(Beat::valueOf).forEach(session.recentBeats()::add);
        }
        session.characters().addAll(storyCharacters);
        session.openThreads().addAll(openThreads);
        session.nodes().clear();
        session.nodes().addAll(nodes.stream().map(StoryNodeEntity::toDomain).toList());
        return session;
    }
}
