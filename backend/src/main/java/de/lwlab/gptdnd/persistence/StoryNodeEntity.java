package de.lwlab.gptdnd.persistence;

import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.DiceRoll;
import de.lwlab.gptdnd.domain.StoryNode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_nodes")
public class StoryNodeEntity {
    @Id
    private String id;

    private String type;

    @Column(length = 500)
    private String action;

    @Column(length = 5000)
    private String text;

    private String rollText;

    private String diceLabel;
    private Integer diceNatural;
    private Integer diceModifier;
    private Integer diceDifficulty;
    private String diceOutcome;

    @Enumerated(EnumType.STRING)
    private Consequence.Tone diceTone;

    @Column(length = 1000)
    private String imagePrompt;

    private Instant createdAt;

    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSessionEntity session;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "story_node_consequences", joinColumns = @JoinColumn(name = "story_node_id"))
    @OrderColumn(name = "position")
    private List<ConsequenceValue> consequences = new ArrayList<>();

    protected StoryNodeEntity() {
    }

    private StoryNodeEntity(StoryNode node, GameSessionEntity session, int position) {
        this.id = node.id();
        this.type = node.type();
        this.action = node.action();
        this.text = node.text();
        this.rollText = node.rollText();
        if (node.roll() != null) {
            this.diceLabel = node.roll().label();
            this.diceNatural = node.roll().natural();
            this.diceModifier = node.roll().modifier();
            this.diceDifficulty = node.roll().difficulty();
            this.diceOutcome = node.roll().outcome();
            this.diceTone = node.roll().tone();
        }
        this.imagePrompt = node.imagePrompt();
        this.createdAt = node.createdAt();
        this.position = position;
        this.session = session;
        this.consequences = node.consequences().stream().map(ConsequenceValue::fromDomain).toList();
    }

    public static StoryNodeEntity fromDomain(StoryNode node, GameSessionEntity session, int position) {
        return new StoryNodeEntity(node, session, position);
    }

    public StoryNode toDomain() {
        return new StoryNode(
                id,
                type,
                action,
                text,
                rollText,
                diceNatural == null ? null : new DiceRoll(diceLabel, diceNatural, diceModifier, diceDifficulty, diceOutcome, diceTone),
                imagePrompt,
                consequences.stream().map(ConsequenceValue::toDomain).toList(),
                createdAt);
    }

    @Embeddable
    public static class ConsequenceValue {
        @Column(length = 500)
        private String text;

        @Enumerated(EnumType.STRING)
        private Consequence.Tone tone;

        protected ConsequenceValue() {
        }

        private ConsequenceValue(String text, Consequence.Tone tone) {
            this.text = text;
            this.tone = tone;
        }

        static ConsequenceValue fromDomain(Consequence consequence) {
            return new ConsequenceValue(consequence.text(), consequence.tone());
        }

        Consequence toDomain() {
            return new Consequence(text, tone);
        }
    }
}
