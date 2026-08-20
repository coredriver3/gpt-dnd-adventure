package de.lwlab.gptdnd.domain;

public record Consequence(String text, Tone tone) {
    public enum Tone {
        NEUTRAL,
        GOOD,
        BAD
    }
}
