package de.lwlab.gptdnd.service.rules;

public enum Degree {
    CRITICAL_FAILURE("critical failure", -4, 0, -2, 0),
    FAILURE("failure", -3, 0, -2, 0),
    PARTIAL_SUCCESS("success with cost", -2, 0, -1, 0),
    SUCCESS("success", -1, 1, 0, 1),
    CRITICAL_SUCCESS("critical success", 0, 2, 0, 1),
    NO_ROLL("no roll needed", -1, 1, -1, 1);

    private final String label;
    private final int minHealth;
    private final int maxHealth;
    private final int minResolve;
    private final int maxResolve;

    Degree(String label, int minHealth, int maxHealth, int minResolve, int maxResolve) {
        this.label = label;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.minResolve = minResolve;
        this.maxResolve = maxResolve;
    }

    public String label() {
        return label;
    }

    public int clampHealth(int delta) {
        return Math.max(minHealth, Math.min(maxHealth, delta));
    }

    public int clampResolve(int delta) {
        return Math.max(minResolve, Math.min(maxResolve, delta));
    }

    public boolean allowsDeath() {
        return this == CRITICAL_FAILURE || this == FAILURE;
    }

    public String limits() {
        return "healthDelta %d..%d, resolveDelta %d..%d%s".formatted(minHealth, maxHealth, minResolve, maxResolve,
                allowsDeath() ? ", characterDied may be true only if the scene is truly lethal" : ", characterDied must be false");
    }

    public static Degree of(int natural, int total, int difficulty) {
        if (natural == 1) {
            return CRITICAL_FAILURE;
        }
        if (natural == 20) {
            return CRITICAL_SUCCESS;
        }
        if (total >= difficulty) {
            return SUCCESS;
        }
        return total >= difficulty - 3 ? PARTIAL_SUCCESS : FAILURE;
    }
}
