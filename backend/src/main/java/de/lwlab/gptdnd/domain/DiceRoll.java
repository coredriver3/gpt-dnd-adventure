package de.lwlab.gptdnd.domain;

/** A d20 roll the client can animate. {@code difficulty} is null when the roll has no target number. */
public record DiceRoll(String label, int natural, int modifier, Integer difficulty, String outcome, Consequence.Tone tone) {
}
