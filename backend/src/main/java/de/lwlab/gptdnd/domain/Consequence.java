package de.lwlab.gptdnd.domain;

public record Consequence(String text, Tone tone) {
    /** Resource key and the text format that {@code GameService} writes for coin changes, e.g. "+3 coin". */
    public static final String COIN = "coin";

    /** True if this consequence records newly found coins. Kept here so writer and reader share one format. */
    public boolean foundCoins() {
        return text != null && text.matches("\\+\\d+ " + COIN);
    }

    public enum Tone {
        NEUTRAL,
        GOOD,
        BAD
    }
}
