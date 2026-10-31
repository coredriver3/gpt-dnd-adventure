package de.lwlab.gptdnd.api;

public record DiceRollResponse(String label, int natural, int modifier, Integer difficulty, String outcome, String tone) {
}
