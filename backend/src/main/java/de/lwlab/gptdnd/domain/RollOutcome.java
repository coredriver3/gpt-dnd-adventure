package de.lwlab.gptdnd.domain;

public record RollOutcome(int min, int max, String grade, int healthDelta, int resolveDelta) {
    public boolean matches(int value) {
        return value >= min && value <= max;
    }
}
